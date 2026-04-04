package com.umc.devine.infrastructure.chat.listener;

import com.umc.devine.domain.chat.repository.ChatMessageRepository;
import com.umc.devine.infrastructure.sse.core.SseEmitterManager;
import com.umc.devine.infrastructure.sse.core.SseEventType;
import com.umc.devine.infrastructure.sse.listener.SseConnectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSseConnectedEventListener {

    private final ChatMessageRepository chatMessageRepository;
    private final SseEmitterManager sseEmitterManager;

    @Async
    @EventListener
    public void handleSseConnected(SseConnectedEvent event) {
        Long memberId = event.getMemberId();
        long count = chatMessageRepository.countRoomsWithUnreadMessages(memberId);
        if (count > 0) {
            sseEmitterManager.sendWithId(
                    memberId,
                    null,
                    SseEventType.CHAT_UNREAD_ROOMS.getEventName(),
                    Map.of("unreadRoomCount", count)
            );
            log.debug("SSE 채팅 뱃지 초기화 전송 - memberId: {}, unreadRoomCount: {}", memberId, count);
        }
    }
}
