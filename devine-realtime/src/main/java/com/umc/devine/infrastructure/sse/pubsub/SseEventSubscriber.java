package com.umc.devine.infrastructure.sse.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.infrastructure.sse.core.SseEmitterManager;
import com.umc.devine.infrastructure.redis.dto.SseEventPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * Redis Pub/Sub 메시지 구독 및 SSE 전송
 */
@Component
@Slf4j
public class SseEventSubscriber implements MessageListener {

    private final SseEmitterManager emitterManager;
    private final ObjectMapper objectMapper;
    private final Executor sseDispatchExecutor;

    public SseEventSubscriber(
            SseEmitterManager emitterManager,
            ObjectMapper objectMapper,
            @Qualifier("sseDispatchExecutor") Executor sseDispatchExecutor
    ) {
        this.emitterManager = emitterManager;
        this.objectMapper = objectMapper;
        this.sseDispatchExecutor = sseDispatchExecutor;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        SseEventPayload payload;
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            payload = objectMapper.readValue(body, SseEventPayload.class);

            log.debug("Redis Sub 수신 - receiverId: {}, eventType: {}",
                    payload.receiverId(), payload.eventType());

            // SSE 전송을 비동기로 처리하여 블로킹 방지
            dispatchSseAsync(payload);

        } catch (JsonProcessingException e) {
            log.error("Redis Sub 메시지 파싱 실패 - error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Redis Sub 처리 중 예외 발생 - error: {}", e.getMessage(), e);
        }
    }

    /**
     * SSE 전송을 비동기로 처리
     * 한 사용자의 네트워크 지연이 다른 메시지 처리를 블로킹하지 않음
     */
    private void dispatchSseAsync(SseEventPayload payload) {
        sseDispatchExecutor.execute(() -> {
            try {
                emitterManager.sendWithId(
                        payload.receiverId(),
                        payload.eventId(),
                        payload.eventType(),
                        payload.data()
                );
            } catch (Exception e) {
                log.error("SSE 전송 실패 - receiverId: {}, error: {}",
                        payload.receiverId(), e.getMessage());
            }
        });
    }
}
