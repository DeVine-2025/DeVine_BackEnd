package com.umc.devine.infrastructure.redis.dto;

import lombok.Builder;

@Builder
public record SseEventPayload(
        String eventId,
        String eventType,
        Long receiverId,
        Object data
) {}
