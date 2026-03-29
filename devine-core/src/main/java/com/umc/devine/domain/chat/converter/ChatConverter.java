package com.umc.devine.domain.chat.converter;

import com.umc.devine.domain.chat.dto.ChatResDTO;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;

public class ChatConverter {

    public static ChatResDTO.OtherMemberInfo toOtherMemberInfo(Member member) {
        return ChatResDTO.OtherMemberInfo.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .image(member.getImage())
                .mainType(member.getMainType().name())
                .build();
    }

    public static ChatResDTO.ChatRoomInfo toChatRoomInfo(ChatRoom room, Long memberId) {
        Member other = room.getOtherMember(memberId);
        return ChatResDTO.ChatRoomInfo.builder()
                .roomId(room.getId())
                .otherMember(toOtherMemberInfo(other))
                .build();
    }

    public static ChatResDTO.ChatMessageDetail toMessageDetail(ChatMessage message) {
        return ChatResDTO.ChatMessageDetail.builder()
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .senderNickname(message.getSender().getNickname())
                .senderImage(message.getSender().getImage())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static ChatResDTO.MessageList toMessageList(Slice<ChatMessage> slice, Integer currentPage) {
        List<ChatResDTO.ChatMessageDetail> messages = slice.getContent().stream()
                .map(ChatConverter::toMessageDetail)
                .toList();
        return ChatResDTO.MessageList.builder()
                .messages(messages)
                .hasNext(slice.hasNext())
                .currentPage(currentPage)
                .build();
    }

    public static ChatResDTO.ChatRoomDetail toChatRoomDetail(
            ChatRoom room, Long memberId, String lastMessage,
            LocalDateTime lastMessageAt, long unreadCount) {
        Member other = room.getOtherMember(memberId);
        return ChatResDTO.ChatRoomDetail.builder()
                .roomId(room.getId())
                .otherMember(toOtherMemberInfo(other))
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount)
                .build();
    }
}
