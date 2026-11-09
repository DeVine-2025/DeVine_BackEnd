package com.umc.devine.domain.chat;

import com.umc.devine.domain.chat.converter.ChatConverter;
import com.umc.devine.domain.chat.dto.ChatResDTO;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.exception.ChatException;
import com.umc.devine.domain.chat.exception.code.ChatErrorReason;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.chat.service.command.ChatCommandService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 시나리오 T7 */
class ChatWithdrawnMemberTest extends IntegrationTestSupport {

    @Autowired private ChatCommandService chatCommandService;
    @Autowired private ChatRoomRepository chatRoomRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private EntityManager em;

    private Member active;
    private Member withdrawn;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        active = memberRepository.save(Member.builder()
                .clerkId("clerk_chat_active").nickname("chatactive")
                .mainType(MemberMainType.PM).used(MemberStatus.ACTIVE).build());
        withdrawn = memberRepository.save(Member.builder()
                .clerkId("clerk_chat_withdrawn").nickname("chatwithdrawn").image("https://cdn/p.png")
                .mainType(MemberMainType.DEVELOPER).used(MemberStatus.ACTIVE).build());
        room = chatRoomRepository.save(ChatRoom.builder().member1(active).member2(withdrawn).build());
        room.leave(withdrawn.getId());
        withdrawn.anonymizeForWithdrawal();
        em.flush();
    }

    @Test
    @DisplayName("T7: 탈퇴한 회원에게는 메시지를 보낼 수 없고, 방에 다시 들어오게 하지도 않는다")
    void cannotSendToWithdrawnMember() {
        assertThatThrownBy(() -> chatCommandService.sendMessage(active.getId(), room.getId(), "안녕하세요"))
                .isInstanceOf(ChatException.class)
                .extracting("reason").isEqualTo(ChatErrorReason.RECEIVER_WITHDRAWN);

        assertThat(room.getMember2Left()).isTrue();
        assertThat(chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(room.getId())).isEmpty();
    }

    @Test
    @DisplayName("T7: 채팅방 상대가 탈퇴했으면 '탈퇴한 회원', 기본 이미지, withdrawn=true로 보인다")
    void withdrawnMemberDisplayed() {
        ChatResDTO.OtherMemberInfo info = ChatConverter.toChatRoomInfo(room, active.getId()).otherMember();

        assertThat(info.nickname()).isEqualTo("탈퇴한 회원");
        assertThat(info.image()).isNull();
        assertThat(info.clerkId()).isNull();
        assertThat(info.withdrawn()).isTrue();
    }
}
