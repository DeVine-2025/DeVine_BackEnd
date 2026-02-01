package com.umc.devine.infrastructure.sse.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.infrastructure.sse.dto.SseEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub을 통한 SSE 이벤트 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseEventPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.channel.notification-prefix}")
    private String notificationChannelPrefix;

    /**
     * 특정 유저에게 알림 이벤트 발행
     */
    public void publish(Long receiverId, SseEventPayload payload) {
        try {
            String channel = notificationChannelPrefix + receiverId;
            String message = objectMapper.writeValueAsString(payload);

            redisTemplate.convertAndSend(channel, message);
            log.debug("Redis Pub 발행 - channel: {}, eventType: {}", channel, payload.eventType());

        } catch (JsonProcessingException e) {
            log.error("Redis Pub 직렬화 실패 - receiverId: {}, error: {}", receiverId, e.getMessage());
        }
    }

    /**
     * 알림 이벤트 발행 (편의 메서드)
     */
    public void publishNotification(Long receiverId, String eventId, Object data) {
        SseEventPayload payload = SseEventPayload.builder()
                .eventId(eventId)
                .eventType(SseEventType.NOTIFICATION.getEventName())
                .receiverId(receiverId)
                .data(data)
                .build();

        publish(receiverId, payload);
    }
}
