package com.umc.devine.domain.chat.converter;

import com.umc.devine.domain.chat.dto.ChatResDTO;
import com.umc.devine.domain.chat.entity.ChatMessage;
import com.umc.devine.domain.chat.entity.ChatRoom;
import com.umc.devine.domain.chat.helper.ChatMessageBlindPolicy;
import com.umc.devine.domain.member.entity.Member;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class ChatConverter {

    public static ChatResDTO.OtherMemberInfo toOtherMemberInfo(Member member) {
        return ChatResDTO.OtherMemberInfo.builder()
                .clerkId(member.getClerkId())
                .nickname(member.getDisplayNickname())
                .image(member.getDisplayImage())
                .mainType(member.getMainType().name())
                .withdrawn(member.isWithdrawn())
                .build();
    }

    public static ChatResDTO.ChatRoomInfo toChatRoomInfo(ChatRoom room, Long memberId) {
        Member other = room.getOtherMember(memberId);
        return ChatResDTO.ChatRoomInfo.builder()
                .roomId(room.getId())
                .otherMember(toOtherMemberInfo(other))
                .build();
    }

    public static ChatResDTO.ChatMessageDetail toMessageDetail(ChatMessage message, Set<Long> reportedByViewer) {
        return ChatResDTO.ChatMessageDetail.builder()
                .messageId(message.getId())
                .senderClerkId(message.getSender().getClerkId())
                .senderNickname(message.getSender().getDisplayNickname())
                .senderImage(message.getSender().getDisplayImage())
                .content(ChatMessageBlindPolicy.resolveContent(message, reportedByViewer))
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .senderWithdrawn(message.getSender().isWithdrawn())
                .build();
    }

    public static ChatResDTO.MessageList toMessageList(
            Slice<ChatMessage> slice, Integer currentPage, Set<Long> reportedByViewer, boolean canSend) {
        List<ChatResDTO.ChatMessageDetail> messages = slice.getContent().stream()
                .map(message -> toMessageDetail(message, reportedByViewer))
                .toList();
        return ChatResDTO.MessageList.builder()
                .messages(messages)
                .hasNext(slice.hasNext())
                .currentPage(currentPage)
                .canSend(canSend)
                .build();
    }

    public static ChatResDTO.ChatRoomDetail toChatRoomDetail(
            ChatRoom room, Long memberId, String lastMessage,
            LocalDateTime lastMessageAt, long unreadCount, boolean canSend) {
        Member other = room.getOtherMember(memberId);
        return ChatResDTO.ChatRoomDetail.builder()
                .roomId(room.getId())
                .otherMember(toOtherMemberInfo(other))
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount)
                .canSend(canSend)
                .build();
    }
}
