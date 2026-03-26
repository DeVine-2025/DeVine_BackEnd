package com.umc.devine.domain.notification.service.query;

import com.umc.devine.domain.notification.converter.NotificationConverter;
import com.umc.devine.domain.notification.dto.NotificationResDTO;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResDTO.NotificationList getNotifications(
            Long memberId,
            Boolean unreadOnly,
            Pageable pageable
    ) {
        Slice<Notification> notifications = unreadOnly
                ? notificationRepository.findUnreadByReceiverId(memberId, pageable)
                : notificationRepository.findAllByReceiverId(memberId, pageable);

        return NotificationConverter.toNotificationList(notifications, pageable.getPageNumber());
    }

    @Override
    public NotificationResDTO.UnreadCount getUnreadCount(Long memberId) {
        long count = notificationRepository.countByReceiverIdAndIsReadFalse(memberId);
        return NotificationConverter.toUnreadCount(count);
    }
}
