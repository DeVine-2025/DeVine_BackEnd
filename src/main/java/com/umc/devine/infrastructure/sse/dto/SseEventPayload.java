package com.umc.devine.infrastructure.sse.dto;

import lombok.Builder;

@Builder
public record SseEventPayload(
        String eventId,
        String eventType,
        Long receiverId,
        Object data
) {}
