package com.umc.devine.infrastructure.sse.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.infrastructure.sse.dto.SseEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 메시지 구독 및 SSE 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseEventSubscriber implements MessageListener {

    private final SseEmitterManager emitterManager;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        SseEventPayload payload = null;
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            payload = objectMapper.readValue(body, SseEventPayload.class);

            log.debug("Redis Sub 수신 - receiverId: {}, eventType: {}",
                    payload.receiverId(), payload.eventType());

            // 해당 유저에게 SSE 전송
            emitterManager.sendWithId(
                    payload.receiverId(),
                    payload.eventId(),
                    payload.eventType(),
                    payload.data()
            );

        } catch (JsonProcessingException e) {
            log.error("Redis Sub 메시지 직렬화 실패 - message: {}, error: {}", message.toString(), e.getMessage());
        } catch (Exception e) {
            String receiverId = (payload != null) ? String.valueOf(payload.receiverId()) : "N/A";
            log.error("Redis Sub 처리 중 예기치 않은 오류 발생 - receiverId: {}, error: {}", receiverId, e.getMessage(), e);
        }
    }
}
