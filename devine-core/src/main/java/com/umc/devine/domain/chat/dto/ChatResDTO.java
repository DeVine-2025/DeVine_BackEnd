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
            String clerkId,
            String nickname,
            String image,
            String mainType,
            // 탈퇴한 회원이면 true. 화면은 프로필 링크와 입력창을 막는다
            boolean withdrawn
    ) {}

    @Builder
    public record ChatMessageDetail(
            Long messageId,
            String senderClerkId,
            String senderNickname,
            String senderImage,
            String content,
            Boolean isRead,
            LocalDateTime createdAt,
            boolean senderWithdrawn
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
