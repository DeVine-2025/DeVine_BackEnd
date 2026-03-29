package com.umc.devine.infrastructure.chat.registry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionRegistry {

    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    @Getter
    @AllArgsConstructor
    public static class SessionInfo {
        private final Long memberId;
        private final Set<Long> roomIds;
    }

    public void registerSession(String sessionId, Long memberId) {
        sessions.put(sessionId, new SessionInfo(memberId, ConcurrentHashMap.newKeySet()));
        log.debug("Session registered - sessionId: {}, memberId: {}", sessionId, memberId);
    }

    public void addRoom(String sessionId, Long roomId) {
        SessionInfo info = sessions.get(sessionId);
        if (info != null) {
            info.getRoomIds().add(roomId);
            log.debug("Room added to session - sessionId: {}, roomId: {}", sessionId, roomId);
        }
    }

    public void removeRoom(String sessionId, Long roomId) {
        SessionInfo info = sessions.get(sessionId);
        if (info != null) {
            info.getRoomIds().remove(roomId);
            log.debug("Room removed from session - sessionId: {}, roomId: {}", sessionId, roomId);
        }
    }

    public SessionInfo removeSession(String sessionId) {
        SessionInfo info = sessions.remove(sessionId);
        if (info != null) {
            log.debug("Session removed - sessionId: {}, memberId: {}, rooms: {}",
                    sessionId, info.getMemberId(), info.getRoomIds());
        }
        return info;
    }

    public SessionInfo getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public Map<String, SessionInfo> getAllSessions() {
        return Collections.unmodifiableMap(sessions);
    }
}
