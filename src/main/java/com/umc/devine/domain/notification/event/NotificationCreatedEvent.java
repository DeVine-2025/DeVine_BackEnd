package com.umc.devine.domain.notification.event;

import com.umc.devine.domain.notification.dto.NotificationResDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 알림 생성 완료 이벤트
 * 트랜잭션 커밋 후 SSE 전송을 위해 사용
 */
@Getter
public class NotificationCreatedEvent extends ApplicationEvent {

    private final Long receiverId;
    private final String eventId;
    private final NotificationResDTO.NotificationDetail detail;

    public NotificationCreatedEvent(Object source, Long receiverId, String eventId,
                                    NotificationResDTO.NotificationDetail detail) {
        super(source);
        this.receiverId = receiverId;
        this.eventId = eventId;
        this.detail = detail;
    }
}
