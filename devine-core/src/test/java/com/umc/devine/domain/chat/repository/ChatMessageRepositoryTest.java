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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 채팅방 나가기 후 재연락(방 재사용) 시, 나간 유저 기준으로 방 목록의
 * lastMessage / 안읽음 수가 leftAt 이후 메시지만 반영하는지 검증한다. (#312)
 */
class ChatMessageRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager em;

    private static final AtomicInteger SEQ = new AtomicInteger();

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
        // member1 = A(나갔다 재입장), member2 = B(그대로)
        room = chatRoomRepository.save(ChatRoom.builder()
                .member1(memberA)
                .member2(memberB)
                .build());
    }

    private Member saveMember() {
        int n = SEQ.incrementAndGet();
        return memberRepository.save(Member.builder()
                .clerkId("clerk_" + n)
                .name("member" + n)
                .nickname("nick_" + n)
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    private ChatMessage saveMessage(Member sender, String content, boolean read, LocalDateTime createdAt) {
        ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .isRead(read)
                .build();
        chatMessageRepository.save(msg);
        em.flush();
        jdbcTemplate.update(
                "UPDATE chat_message SET created_at = ? WHERE chat_message_id = ?",
                createdAt, msg.getId());
        return msg;
    }

    /** A(member1)가 leftAt 시점에 나갔다가 재입장한 상태로 설정한다. */
    private void memberALeftAndRejoined(LocalDateTime leftAt) {
        jdbcTemplate.update(
                "UPDATE chat_room SET member1_left = false, member1_left_at = ? WHERE chat_room_id = ?",
                leftAt, room.getId());
        em.clear();
    }

    @Nested
    @DisplayName("findLastMessagesByRoomIds - 나간 유저 기준 lastMessage 리셋")
    class FindLastMessages {

        @Test
        @DisplayName("나갔다 재입장한 유저에게는 leftAt 이전 메시지가 lastMessage로 잡히지 않는다")
        void excludesMessagesBeforeLeft_forLeftMember() {
            // given: 나가기 전 메시지만 존재, A가 나갔다 재입장
            saveMessage(memberB, "old", true, T_OLD);
            memberALeftAndRejoined(T_LEFT);

            // when
            List<ChatMessage> result = chatMessageRepository.findLastMessagesByRoomIds(
                    List.of(room.getId()), memberA.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("나가지 않은 상대방에게는 이전 메시지가 그대로 lastMessage로 잡힌다")
        void keepsMessages_forNonLeftMember() {
            // given
            saveMessage(memberB, "old", true, T_OLD);
            memberALeftAndRejoined(T_LEFT);

            // when
            List<ChatMessage> result = chatMessageRepository.findLastMessagesByRoomIds(
                    List.of(room.getId()), memberB.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("old");
        }

        @Test
        @DisplayName("재입장 후 새 메시지가 오면 그 메시지가 lastMessage로 잡힌다")
        void showsMessagesAfterRejoin() {
            // given
            saveMessage(memberB, "old", true, T_OLD);
            memberALeftAndRejoined(T_LEFT);
            saveMessage(memberB, "new", false, T_NEW);

            // when
            List<ChatMessage> result = chatMessageRepository.findLastMessagesByRoomIds(
                    List.of(room.getId()), memberA.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContent()).isEqualTo("new");
        }
    }

    @Nested
    @DisplayName("countUnreadPerRoom - 나간 유저 기준 안읽음 카운트")
    class CountUnreadPerRoom {

        @Test
        @DisplayName("leftAt 이전 안읽음 메시지는 카운트하지 않는다")
        void excludesUnreadBeforeLeft() {
            // given: 나가기 전 안읽음 메시지만 존재
            saveMessage(memberB, "old-unread", false, T_OLD);
            memberALeftAndRejoined(T_LEFT);

            // when
            List<Object[]> result = chatMessageRepository.countUnreadPerRoom(
                    List.of(room.getId()), memberA.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("재입장 후 도착한 안읽음 메시지는 카운트한다")
        void countsUnreadAfterRejoin() {
            // given
            saveMessage(memberB, "old-unread", false, T_OLD);
            memberALeftAndRejoined(T_LEFT);
            saveMessage(memberB, "new-unread", false, T_NEW);

            // when
            List<Object[]> result = chatMessageRepository.countUnreadPerRoom(
                    List.of(room.getId()), memberA.getId());

            // then
            assertThat(result).hasSize(1);
            assertThat(((Number) result.get(0)[1]).longValue()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("countRoomsWithUnreadMessages - 나간 유저 기준 전체 안읽음 방 수")
    class CountRoomsWithUnread {

        @Test
        @DisplayName("leftAt 이전 안읽음만 있으면 안읽음 방으로 세지 않는다")
        void excludesRoomWithOnlyPreLeftUnread() {
            // given
            saveMessage(memberB, "old-unread", false, T_OLD);
            memberALeftAndRejoined(T_LEFT);

            // when
            long count = chatMessageRepository.countRoomsWithUnreadMessages(memberA.getId());

            // then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("재입장 후 안읽음 메시지가 있으면 안읽음 방으로 센다")
        void countsRoomWithPostRejoinUnread() {
            // given
            saveMessage(memberB, "old-unread", false, T_OLD);
            memberALeftAndRejoined(T_LEFT);
            saveMessage(memberB, "new-unread", false, T_NEW);

            // when
            long count = chatMessageRepository.countRoomsWithUnreadMessages(memberA.getId());

            // then
            assertThat(count).isEqualTo(1L);
        }
    }
}