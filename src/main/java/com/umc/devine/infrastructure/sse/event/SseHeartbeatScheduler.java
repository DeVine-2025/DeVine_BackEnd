package com.umc.devine.infrastructure.sse.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SSE 연결 유지를 위한 주기적 하트비트 전송
 *
 * 브라우저/프록시의 유휴 연결 타임아웃 방지 목적
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseHeartbeatScheduler {

    private final SseEmitterManager emitterManager;

    /**
     * 30초마다 모든 SSE 연결에 하트비트 전송
     */
    @Scheduled(fixedRateString = "${sse.heartbeat-rate}")
    public void sendHeartbeat() {
        int connectionCount = emitterManager.getConnectionCount();
        if (connectionCount > 0) {
            log.debug("SSE 하트비트 전송 - 연결 수: {}", connectionCount);
            emitterManager.sendHeartbeatToAll();
        }
    }
}
