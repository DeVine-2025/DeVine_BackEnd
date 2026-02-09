package com.umc.devine.domain.notification.event;

import com.umc.devine.infrastructure.sse.pubsub.SseEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 알림 생성 이벤트 리스너
 * 트랜잭션 커밋 후 Redis Pub/Sub으로 SSE 이벤트 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationCreatedEventListener {

    private final SseEventPublisher sseEventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationCreatedEvent event) {
        sseEventPublisher.publishNotification(
                event.getReceiverId(),
                event.getEventId(),
                event.getDetail()
        );

        log.debug("알림 SSE 이벤트 발행 - receiverId: {}, eventId: {}",
                event.getReceiverId(), event.getEventId());
    }
}
