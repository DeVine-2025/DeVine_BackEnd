package com.umc.devine.infrastructure.sse.core;

import lombok.Getter;

@Getter
public enum SseEventType {
    CONNECT("connect"),
    NOTIFICATION("notification"),
    HEARTBEAT("heartbeat"),
    SHUTDOWN("shutdown");

    private final String eventName;

    SseEventType(String eventName) {
        this.eventName = eventName;
    }
}
