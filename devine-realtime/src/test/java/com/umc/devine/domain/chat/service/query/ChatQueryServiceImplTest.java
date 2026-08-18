package com.umc.devine.domain.chat.service.query;

import com.umc.devine.domain.chat.dto.ChatResDTO;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.domain.chat.repository.ChatRoomRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatQueryServiceImpl.getRoomList - lastMessage/lastMessageAt 짝 처리")
class ChatQueryServiceImplTest {

    private static final Long ME = 1L;
    private static final Long OTHER = 2L;
    private static final Long ROOM_WITH_MSG = 10L;
    private static final Long ROOM_EMPTY = 20L;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatQueryServiceImpl chatQueryService;

    private Member member(Long id, MemberMainType type) {
        return Member.builder()
                .id(id)
                .clerkId("clerk-" + id)
                .nickname("nick-" + id)
                .mainType(type)
                .build();
    }

    private ChatRoom room(Long id, Member m1, Member m2) {
        return ChatRoom.builder().id(id).member1(m1).member2(m2).build();
    }

    private ChatMessage message(ChatRoom room, Member sender, String content, LocalDateTime createdAt) {
        ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build();
        ReflectionTestUtils.setField(msg, "createdAt", createdAt);
        return msg;
    }

    @Test
    @DisplayName("마지막 메시지가 있는 방은 그 메시지의 시각이 lastMessageAt이 된다")
    void lastMessageAtMatchesLastMessage() {
        Member me = member(ME, MemberMainType.DEVELOPER);
        Member other = member(OTHER, MemberMainType.PM);
        ChatRoom room = room(ROOM_WITH_MSG, me, other);
        LocalDateTime msgAt = LocalDateTime.of(2026, 7, 25, 3, 0);
        ChatMessage lastMsg = message(room, other, "안녕", msgAt);

        // row[1](last_activity)은 이제 표시에 쓰이지 않으므로 null이어도 무방함을 확인
        when(chatRoomRepository.findActiveRoomIdsSortedByActivity(ME))
                .thenReturn(List.<Object[]>of(new Object[]{ROOM_WITH_MSG, null}));
        when(chatRoomRepository.findRoomsWithMembersByIds(anyList()))
                .thenReturn(List.of(room));
        when(chatMessageRepository.countUnreadPerRoom(anyList(), eq(ME)))
                .thenReturn(List.<Object[]>of(new Object[]{ROOM_WITH_MSG, 2L}));
        when(chatMessageRepository.findLastMessagesByRoomIds(anyList(), eq(ME)))
                .thenReturn(List.of(lastMsg));

        ChatResDTO.ChatRoomList result = chatQueryService.getRoomList(ME);

        assertThat(result.rooms()).hasSize(1);
        ChatResDTO.ChatRoomDetail detail = result.rooms().get(0);
        assertThat(detail.lastMessage()).isEqualTo("안녕");
        assertThat(detail.lastMessageAt()).isEqualTo(msgAt);
        assertThat(detail.unreadCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("마지막 메시지가 없는 방(재입장 후 새 메시지 없음)은 lastMessage와 lastMessageAt이 모두 null이다")
    void lastMessageAtNullWhenNoLastMessage() {
        Member me = member(ME, MemberMainType.DEVELOPER);
        Member other = member(OTHER, MemberMainType.PM);
        ChatRoom emptyRoom = room(ROOM_EMPTY, me, other);

        when(chatRoomRepository.findActiveRoomIdsSortedByActivity(ME))
                .thenReturn(List.<Object[]>of(new Object[]{ROOM_EMPTY, null}));
        when(chatRoomRepository.findRoomsWithMembersByIds(anyList()))
                .thenReturn(List.of(emptyRoom));
        when(chatMessageRepository.countUnreadPerRoom(anyList(), eq(ME)))
                .thenReturn(List.of());
        when(chatMessageRepository.findLastMessagesByRoomIds(anyList(), eq(ME)))
                .thenReturn(List.of());

        ChatResDTO.ChatRoomList result = chatQueryService.getRoomList(ME);

        assertThat(result.rooms()).hasSize(1);
        ChatResDTO.ChatRoomDetail detail = result.rooms().get(0);
        assertThat(detail.lastMessage()).isNull();
        assertThat(detail.lastMessageAt()).isNull();
        assertThat(detail.unreadCount()).isEqualTo(0L);
    }
}
