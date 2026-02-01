package com.umc.devine.domain.notification.event;

import com.umc.devine.domain.notification.converter.NotificationConverter;
import com.umc.devine.domain.notification.dto.NotificationResDTO;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.infrastructure.sse.event.SseEmitterManager;
import com.umc.devine.infrastructure.sse.event.SseConnectedEvent;
import com.umc.devine.infrastructure.sse.event.SseEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SSE 재연결 시 놓친 알림 전송을 담당하는 이벤트 리스너
 * Notification 도메인이 SSE 인프라 이벤트를 구독하는 방식으로 느슨한 결합 유지
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseConnectedEventListener {

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

        List<Notification> missedNotifications =
                notificationRepository.findMissedNotifications(memberId, lastEventId);

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

        log.info("놓친 알림 전송 완료 - memberId: {}, lastEventId: {}, count: {}",
                memberId, lastEventId, missedNotifications.size());
    }
}
