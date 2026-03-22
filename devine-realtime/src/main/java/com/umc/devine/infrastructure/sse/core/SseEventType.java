package com.umc.devine.infrastructure.sse.core;

import com.umc.devine.infrastructure.redis.RedisEventConstants;
import lombok.Getter;

@Getter
public enum SseEventType {
    CONNECT("connect"),
    NOTIFICATION(RedisEventConstants.NOTIFICATION),
    HEARTBEAT("heartbeat"),
    SHUTDOWN("shutdown");

    private final String eventName;

    SseEventType(String eventName) {
        this.eventName = eventName;
    }
}
