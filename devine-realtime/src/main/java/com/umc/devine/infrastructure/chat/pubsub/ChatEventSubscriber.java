package com.umc.devine.infrastructure.chat.pubsub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.infrastructure.redis.ChatEventType;
import com.umc.devine.infrastructure.redis.dto.ChatEventPayload;
import com.umc.devine.infrastructure.sse.core.SseEmitterManager;
import com.umc.devine.infrastructure.sse.core.SseEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class ChatEventSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final SseEmitterManager sseEmitterManager;
    private final ObjectMapper objectMapper;
    private final Executor chatDispatchExecutor;

    public ChatEventSubscriber(
            SimpMessagingTemplate messagingTemplate,
            SseEmitterManager sseEmitterManager,
            ObjectMapper objectMapper,
            @Qualifier("chatDispatchExecutor") Executor chatDispatchExecutor
    ) {
        this.messagingTemplate = messagingTemplate;
        this.sseEmitterManager = sseEmitterManager;
        this.objectMapper = objectMapper;
        this.chatDispatchExecutor = chatDispatchExecutor;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatEventPayload payload = objectMapper.readValue(body, ChatEventPayload.class);
            chatDispatchExecutor.execute(() -> dispatch(payload));
        } catch (JsonProcessingException e) {
            log.error("채팅 Redis 메시지 파싱 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(ChatEventPayload payload) {
        try {
            String eventType = payload.eventType();

            if (ChatEventType.CHAT_MESSAGE.equals(eventType)) {
                handleChatMessage(payload);
            } else if (ChatEventType.CHAT_READ.equals(eventType)) {
                handleChatRead(payload);
            }
        } catch (Exception e) {
            log.error("채팅 이벤트 디스패치 실패 - receiverId: {}", payload.receiverId(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleChatMessage(ChatEventPayload payload) {
        Map<String, Object> data = (Map<String, Object>) payload.data();
        Object roomIdObj = data.get("roomId");
        Long roomId = roomIdObj instanceof Number n ? n.longValue() : Long.parseLong(roomIdObj.toString());

        // Strip unreadRoomCount from WebSocket message
        Map<String, Object> wsData = new HashMap<>(data);
        wsData.remove("unreadRoomCount");

        // WebSocket으로 메시지 전달
        messagingTemplate.convertAndSendToUser(
                payload.receiverClerkId(),
                "/queue/chat/" + roomId + "/messages",
                wsData
        );

        // SSE 뱃지 업데이트
        Object unreadObj = data.get("unreadRoomCount");
        long unreadRoomCount = unreadObj instanceof Number n ? n.longValue() : Long.parseLong(unreadObj.toString());
        if (unreadRoomCount > 0) {
            sseEmitterManager.sendWithId(
                    payload.receiverId(),
                    null,
                    SseEventType.CHAT_UNREAD_ROOMS.getEventName(),
                    Map.of("unreadRoomCount", unreadRoomCount)
            );
        }
    }

    private void handleChatRead(ChatEventPayload payload) {
        messagingTemplate.convertAndSendToUser(
                payload.senderClerkId(),
                "/queue/chat/read",
                payload.data()
        );
    }
}
