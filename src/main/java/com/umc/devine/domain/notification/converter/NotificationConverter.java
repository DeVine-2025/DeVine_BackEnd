package com.umc.devine.domain.notification.converter;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.notification.dto.NotificationResDTO;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.enums.NotificationType;
import org.springframework.data.domain.Slice;

import java.util.List;

public class NotificationConverter {

    public static Notification toNotification(
            NotificationType type,
            Member receiver,
            Member sender,
            String content,
            Long referenceId
    ) {
        return Notification.builder()
                .type(type)
                .title(type.getDefaultTitle())
                .content(content)
                .receiver(receiver)
                .sender(sender)
                .referenceId(referenceId)
                .build();
    }

    public static NotificationResDTO.NotificationDetail toDetail(Notification notification) {
        return NotificationResDTO.NotificationDetail.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .content(notification.getContent())
                .referenceId(notification.getReferenceId())
                .sender(notification.getSender() != null
                        ? toSenderInfo(notification.getSender())
                        : null)
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public static NotificationResDTO.SenderInfo toSenderInfo(Member member) {
        return NotificationResDTO.SenderInfo.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .profileImageUrl(member.getImage())
                .build();
    }

    public static NotificationResDTO.NotificationList toNotificationList(
            Slice<Notification> slice,
            Integer currentPage
    ) {
        List<NotificationResDTO.NotificationDetail> notifications = slice.getContent().stream()
                .map(NotificationConverter::toDetail)
                .toList();

        return NotificationResDTO.NotificationList.builder()
                .notifications(notifications)
                .hasNext(slice.hasNext())
                .currentPage(currentPage)
                .build();
    }

    public static NotificationResDTO.UnreadCount toUnreadCount(Long count) {
        return NotificationResDTO.UnreadCount.builder()
                .count(count)
                .build();
    }

    public static NotificationResDTO.MarkAllReadResult toMarkAllReadResult(Integer markedCount) {
        return NotificationResDTO.MarkAllReadResult.builder()
                .markedCount(markedCount)
                .build();
    }
}
