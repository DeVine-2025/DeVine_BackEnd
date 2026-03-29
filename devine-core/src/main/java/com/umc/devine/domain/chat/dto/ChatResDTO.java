package com.umc.devine.domain.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResDTO {

    @Builder
    public record ChatRoomDetail(
            Long roomId,
            OtherMemberInfo otherMember,
            String lastMessage,
            LocalDateTime lastMessageAt,
            long unreadCount
    ) {}

    @Builder
    public record OtherMemberInfo(
            Long id,
            String nickname,
            String image,
            String mainType
    ) {}

    @Builder
    public record ChatMessageDetail(
            Long messageId,
            Long senderId,
            String senderNickname,
            String senderImage,
            String content,
            Boolean isRead,
            LocalDateTime createdAt
    ) {}

    @Builder
    public record ChatRoomInfo(
            Long roomId,
            OtherMemberInfo otherMember
    ) {}

    @Builder
    public record ChatRoomList(
            List<ChatRoomDetail> rooms
    ) {}

    @Builder
    public record MessageList(
            List<ChatMessageDetail> messages,
            Boolean hasNext,
            Integer currentPage
    ) {}

    @Builder
    public record UnreadRoomCount(
            long unreadRoomCount
    ) {}

    @Builder
    public record ReadResult(
            long unreadRoomCount
    ) {}
}
