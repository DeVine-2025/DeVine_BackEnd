package com.umc.devine.infrastructure.chat.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceCleanupScheduler {

    private final StringRedisTemplate redisTemplate;

    @Value("${redis.presence.key-prefix}")
    private String presenceKeyPrefix;

    @Value("${redis.presence.active-rooms-key}")
    private String activeRoomsKey;

    @Value("${redis.presence.timeout-seconds}")
    private int timeoutSeconds;

    @Scheduled(fixedRate = 30_000)
    public void cleanupExpiredPresence() {
        Set<String> activeRooms = redisTemplate.opsForSet().members(activeRoomsKey);
        if (activeRooms == null || activeRooms.isEmpty()) {
            return;
        }

        double cutoff = System.currentTimeMillis() - (timeoutSeconds * 1000L);

        for (String roomId : activeRooms) {
            String key = presenceKeyPrefix + roomId;
            Long removed = redisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, cutoff);

            if (removed != null && removed > 0) {
                log.info("Presence cleanup - roomId: {}, removed: {}", roomId, removed);
            }

            Long size = redisTemplate.opsForZSet().zCard(key);
            if (size == null || size == 0) {
                redisTemplate.opsForSet().remove(activeRoomsKey, roomId);
            }
        }
    }
}
