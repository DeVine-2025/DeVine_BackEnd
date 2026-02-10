package com.umc.devine.infrastructure.sse.listener;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * SSE 연결 완료 시 발행되는 이벤트
 * 재연결 시 놓친 메시지 처리를 위해 사용
 */
@Getter
public class SseConnectedEvent extends ApplicationEvent {

    private final Long memberId;
    private final Long lastEventId;

    public SseConnectedEvent(Object source, Long memberId, Long lastEventId) {
        super(source);
        this.memberId = memberId;
        this.lastEventId = lastEventId;
    }

    public boolean hasLastEventId() {
        return lastEventId != null;
    }
}
