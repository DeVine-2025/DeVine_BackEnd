package com.umc.devine.infrastructure.sse.listener;

import com.umc.devine.domain.notification.converter.NotificationConverter;
import com.umc.devine.domain.notification.dto.NotificationResDTO;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.infrastructure.sse.core.SseEmitterManager;
import com.umc.devine.infrastructure.sse.core.SseEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SSE 재연결 시 놓친 알림 전송을 담당하는 이벤트 리스너
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseConnectedEventListener {

    /**
     * 재연결 시 한 번에 전송할 최대 알림 개수
     * 너무 많은 알림을 한 번에 전송하면 서버/클라이언트 부하 발생
     */
    private static final int MAX_MISSED_NOTIFICATIONS = 50;

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;

    @Async
    @EventListener
    public void handleSseConnected(SseConnectedEvent event) {
        if (!event.hasLastEventId()) {
            return;
        }

        Long memberId = event.getMemberId();
        Long lastEventId = event.getLastEventId();

        // 최대 개수 제한을 두어 대량 알림으로 인한 부하 방지
        List<Notification> missedNotifications = notificationRepository.findMissedNotifications(
                memberId,
                lastEventId,
                PageRequest.of(0, MAX_MISSED_NOTIFICATIONS)
        );

        for (Notification notification : missedNotifications) {
            NotificationResDTO.NotificationDetail detail =
                    NotificationConverter.toDetail(notification);

            sseEmitterManager.sendWithId(
                    memberId,
                    String.valueOf(notification.getId()),
                    SseEventType.NOTIFICATION.getEventName(),
                    detail
            );
        }

        if (missedNotifications.size() >= MAX_MISSED_NOTIFICATIONS) {
            log.info("놓친 알림 전송 완료 (제한 도달) - memberId: {}, lastEventId: {}, count: {}/{}",
                    memberId, lastEventId, missedNotifications.size(), MAX_MISSED_NOTIFICATIONS);
        } else {
            log.info("놓친 알림 전송 완료 - memberId: {}, lastEventId: {}, count: {}",
                    memberId, lastEventId, missedNotifications.size());
        }
    }
}
