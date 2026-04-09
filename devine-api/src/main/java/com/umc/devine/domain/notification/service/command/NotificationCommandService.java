package com.umc.devine.domain.notification.service.command;

import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.enums.NotificationType;

public interface NotificationCommandService {

    Notification create(
            NotificationType type,
            Long receiverId,
            Long senderId,
            String content,
            Long referenceId
    );

    void markAsRead(Long notificationId, Long memberId);

    int markAllAsRead(Long memberId);
}
