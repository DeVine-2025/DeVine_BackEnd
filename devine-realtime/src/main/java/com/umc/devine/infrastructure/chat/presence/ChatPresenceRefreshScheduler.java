package com.umc.devine.infrastructure.chat.presence;

import com.umc.devine.infrastructure.chat.registry.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceRefreshScheduler {

    private final WebSocketSessionRegistry sessionRegistry;
    private final ChatPresenceManager chatPresenceManager;

    @Scheduled(fixedRate = 10_000)
    public void refreshActivePresence() {
        sessionRegistry.getAllSessions().forEach((sessionId, info) -> {
            for (Long roomId : info.getRoomIds()) {
                chatPresenceManager.refreshPresence(roomId, info.getMemberId());
            }
        });
    }
}
