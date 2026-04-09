package com.umc.devine.domain.notification.service.query;

import com.umc.devine.domain.notification.dto.NotificationResDTO;
import org.springframework.data.domain.Pageable;

public interface NotificationQueryService {

    NotificationResDTO.NotificationList getNotifications(Long memberId, Boolean unreadOnly, Pageable pageable);

    NotificationResDTO.UnreadCount getUnreadCount(Long memberId);
}
