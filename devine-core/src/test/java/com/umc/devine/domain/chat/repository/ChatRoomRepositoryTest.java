package com.umc.devine.domain.chat.repository;

import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.CoreIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 방 목록 정렬/lastMessageAt을 결정하는 last_activity가
 * 나간 유저 기준 leftAt 이후 메시지만 반영하는지 검증한다. (#312)
 */
class ChatRoomRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager em;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private static final LocalDateTime T_ROOM_CREATED = LocalDateTime.of(2026, 1, 1, 9, 0);
    private static final LocalDateTime T_OLD = LocalDateTime.of(2026, 1, 1, 10, 0);
    private static final LocalDateTime T_LEFT = LocalDateTime.of(2026, 1, 1, 11, 0);
    private static final LocalDateTime T_NEW = LocalDateTime.of(2026, 1, 1, 12, 0);

    private Member memberA;
    private Member memberB;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        memberA = saveMember();
        memberB = saveMember();
        room = chatRoomRepository.save(ChatRoom.builder()
                .member1(memberA)
                .member2(memberB)
                .build());
        em.flush();
        jdbcTemplate.update(
                "UPDATE chat_room SET created_at = ? WHERE chat_room_id = ?",
                T_ROOM_CREATED, room.getId());
    }

    private Member saveMember() {
        int n = SEQ.incrementAndGet();
        return memberRepository.save(Member.builder()
                .clerkId("clerk_room_" + n)
                .name("member" + n)
                .nickname("nick_room_" + n)
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    private void saveMessage(Member sender, String content, LocalDateTime createdAt) {
        ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .isRead(true)
                .build();
        chatMessageRepository.save(msg);
        em.flush();
        jdbcTemplate.update(
                "UPDATE chat_message SET created_at = ? WHERE chat_message_id = ?",
                createdAt, msg.getId());
    }

    private void memberALeftAndRejoined(LocalDateTime leftAt) {
        jdbcTemplate.update(
                "UPDATE chat_room SET member1_left = false, member1_left_at = ? WHERE chat_room_id = ?",
                leftAt, room.getId());
        em.clear();
    }

    private LocalDateTime lastActivityOf(Member member) {
        List<Object[]> rows = chatRoomRepository.findActiveRoomIdsSortedByActivity(member.getId());
        assertThat(rows).hasSize(1);
        Object raw = rows.get(0)[1];
        return raw instanceof Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) raw;
    }

    @Test
    @DisplayName("나갔다 재입장한 유저 기준으로 leftAt 이전 메시지는 last_activity에 반영되지 않고 방 생성시각으로 폴백한다")
    void lastActivityExcludesPreLeftMessage() {
        // given: 나가기 전 메시지만 존재, A가 나갔다 재입장
        saveMessage(memberB, "old", T_OLD);
        memberALeftAndRejoined(T_LEFT);

        // when / then: 이전 메시지 시각(T_OLD)이 아니라 방 생성시각으로 폴백
        assertThat(lastActivityOf(memberA)).isEqualTo(T_ROOM_CREATED);
    }

    @Test
    @DisplayName("재입장 후 새 메시지가 오면 그 시각이 last_activity가 된다")
    void lastActivityReflectsPostRejoinMessage() {
        // given
        saveMessage(memberB, "old", T_OLD);
        memberALeftAndRejoined(T_LEFT);
        saveMessage(memberB, "new", T_NEW);

        // when / then
        assertThat(lastActivityOf(memberA)).isEqualTo(T_NEW);
    }

    @Test
    @DisplayName("나가지 않은 상대방에게는 이전 메시지 시각이 그대로 last_activity가 된다")
    void lastActivityKeepsMessageForNonLeftMember() {
        // given
        saveMessage(memberB, "old", T_OLD);
        memberALeftAndRejoined(T_LEFT);

        // when / then
        assertThat(lastActivityOf(memberB)).isEqualTo(T_OLD);
    }
}
