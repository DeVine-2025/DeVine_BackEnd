package com.umc.devine.infrastructure.chat.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceManager {

    private final StringRedisTemplate redisTemplate;

    @Value("${redis.presence.key-prefix}")
    private String presenceKeyPrefix;

    @Value("${redis.presence.active-rooms-key}")
    private String activeRoomsKey;

    public void enterRoom(Long roomId, Long memberId) {
        String key = presenceKeyPrefix + roomId;
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(key, String.valueOf(memberId), score);
        redisTemplate.opsForSet().add(activeRoomsKey, String.valueOf(roomId));
        log.debug("Presence enter - roomId: {}, memberId: {}", roomId, memberId);
    }

    public void leaveRoom(Long roomId, Long memberId) {
        String key = presenceKeyPrefix + roomId;
        redisTemplate.opsForZSet().remove(key, String.valueOf(memberId));
        log.debug("Presence leave - roomId: {}, memberId: {}", roomId, memberId);
    }

    public boolean isInRoom(Long roomId, Long memberId) {
        String key = presenceKeyPrefix + roomId;
        Double score = redisTemplate.opsForZSet().score(key, String.valueOf(memberId));
        return score != null;
    }

    public void refreshPresence(Long roomId, Long memberId) {
        String key = presenceKeyPrefix + roomId;
        double score = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(key, String.valueOf(memberId), score);
    }
}
