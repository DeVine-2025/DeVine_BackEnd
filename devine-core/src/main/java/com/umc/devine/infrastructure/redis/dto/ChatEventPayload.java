package com.umc.devine.infrastructure.redis.dto;

import lombok.Builder;

@Builder
public record ChatEventPayload(
        String eventId,
        String eventType,
        Long receiverId,
        String receiverClerkId,
        String senderClerkId,
        Object data
) {}
