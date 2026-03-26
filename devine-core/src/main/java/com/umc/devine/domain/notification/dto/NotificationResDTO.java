package com.umc.devine.domain.notification.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationResDTO {

    @Builder
    public record NotificationDetail(
            Long id,
            String type,
            String title,
            String content,
            Long referenceId,
            SenderInfo sender,
            Boolean isRead,
            LocalDateTime createdAt
    ) {}

    @Builder
    public record SenderInfo(
            Long id,
            String nickname,
            String profileImageUrl
    ) {}

    @Builder
    public record NotificationList(
            List<NotificationDetail> notifications,
            Boolean hasNext,
            Integer currentPage
    ) {}

    @Builder
    public record UnreadCount(
            Long count
    ) {}

    @Builder
    public record MarkAllReadResult(
            Integer markedCount
    ) {}
}
