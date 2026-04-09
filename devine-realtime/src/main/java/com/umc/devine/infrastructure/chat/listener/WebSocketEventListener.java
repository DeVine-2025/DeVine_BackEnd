package com.umc.devine.infrastructure.chat.listener;

import com.umc.devine.infrastructure.chat.auth.ChatPrincipal;
import com.umc.devine.infrastructure.chat.presence.ChatPresenceManager;
import com.umc.devine.infrastructure.chat.registry.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private static final Pattern ROOM_ID_PATTERN =
            Pattern.compile("/user/queue/chat/(\\d+)/messages");

    private final ChatPresenceManager chatPresenceManager;
    private final WebSocketSessionRegistry sessionRegistry;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        ChatPrincipal principal = (ChatPrincipal) accessor.getUser();
        if (principal != null && accessor.getSessionId() != null) {
            sessionRegistry.registerSession(accessor.getSessionId(), principal.getMemberId());
        }
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        ChatPrincipal principal = (ChatPrincipal) accessor.getUser();

        if (destination == null || principal == null) return;

        Long roomId = extractRoomId(destination);
        if (roomId != null) {
            chatPresenceManager.enterRoom(roomId, principal.getMemberId());
            if (accessor.getSessionId() != null) {
                sessionRegistry.addRoom(accessor.getSessionId(), roomId);
            }
            log.debug("Subscribe - memberId: {}, roomId: {}", principal.getMemberId(), roomId);
        }
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        ChatPrincipal principal = (ChatPrincipal) accessor.getUser();

        if (principal == null || accessor.getSessionId() == null) return;

        String destination = accessor.getDestination();
        if (destination != null) {
            Long roomId = extractRoomId(destination);
            if (roomId != null) {
                chatPresenceManager.leaveRoom(roomId, principal.getMemberId());
                sessionRegistry.removeRoom(accessor.getSessionId(), roomId);
                log.debug("Unsubscribe - memberId: {}, roomId: {}", principal.getMemberId(), roomId);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId == null) return;

        WebSocketSessionRegistry.SessionInfo info = sessionRegistry.removeSession(sessionId);
        if (info != null) {
            for (Long roomId : info.getRoomIds()) {
                chatPresenceManager.leaveRoom(roomId, info.getMemberId());
            }
            log.debug("Disconnect - memberId: {}, rooms: {}", info.getMemberId(), info.getRoomIds());
        }
    }

    private Long extractRoomId(String destination) {
        Matcher matcher = ROOM_ID_PATTERN.matcher(destination);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }
}
