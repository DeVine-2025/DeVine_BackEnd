package com.umc.devine.infrastructure.sse.event;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 연결(SseEmitter) 관리 컴포넌트
 *
 */
@Component
@Slf4j
public class SseEmitterManager {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Value("${sse.timeout}")
    private long sseTimeout;

    /**
     * 새로운 SSE 연결 생성
     * compute()를 사용하여 원자적으로 기존 연결 종료 및 새 연결 생성
     */
    public SseEmitter create(Long memberId) {
        SseEmitter newEmitter = emitters.compute(memberId, (key, existing) -> {
            if (existing != null) {
                existing.complete();
                log.debug("기존 SSE 연결 종료 - memberId: {}", key);
            }
            return new SseEmitter(sseTimeout);
        });

        setupCallbacks(memberId, newEmitter);
        sendConnectEvent(memberId);

        log.info("SSE 연결 생성 - memberId: {}, 총 연결 수: {}", memberId, emitters.size());
        return newEmitter;
    }

    /**
     * 클라이언트에게 이벤트 전송
     */
    public void sendToClient(Long memberId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter == null) {
            log.debug("SSE 연결 없음 - memberId: {}", memberId);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException e) {
            log.warn("SSE 전송 실패 - memberId: {}, error: {}", memberId, e.getMessage());
            removeEmitter(memberId);
        }
    }

    /**
     * Last-Event-ID를 포함한 이벤트 전송 (재연결 시 사용)
     */
    public void sendWithId(Long memberId, String eventId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter == null) {
            return;
        }

        try {
            SseEmitter.SseEventBuilder builder = SseEmitter.event()
                    .name(eventName)
                    .data(data);

            if (eventId != null) {
                builder.id(eventId);
            }

            emitter.send(builder);
        } catch (IOException e) {
            log.warn("SSE 전송 실패 - memberId: {}", memberId);
            removeEmitter(memberId);
        }
    }

    /**
     * 연결 여부 확인
     */
    public boolean isConnected(Long memberId) {
        return emitters.containsKey(memberId);
    }

    /**
     * 현재 연결 수 반환
     */
    public int getConnectionCount() {
        return emitters.size();
    }

    /**
     * 모든 연결에 하트비트 전송
     * Iterator를 사용하여 안전하게 순회하며 실패 시 제거
     */
    public void sendHeartbeatToAll() {
        var iterator = emitters.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                entry.getValue().send(SseEmitter.event()
                        .name(SseEventType.HEARTBEAT.getEventName())
                        .data(Map.of("timestamp", System.currentTimeMillis())));
            } catch (IOException e) {
                log.debug("하트비트 전송 실패 - memberId: {}", entry.getKey());
                iterator.remove();
            }
        }
    }

    /**
     * 모든 연결에 커스텀 이벤트 브로드캐스트
     * @return 전송 성공한 연결 수
     */
    public int broadcast(String eventName, Object data) {
        int successCount = 0;
        var iterator = emitters.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                entry.getValue().send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                successCount++;
            } catch (IOException e) {
                log.debug("브로드캐스트 전송 실패 - memberId: {}", entry.getKey());
                iterator.remove();
            }
        }

        log.info("브로드캐스트 완료 - eventName: {}, 성공: {}/{}", eventName, successCount, successCount + (emitters.size() - successCount));
        return successCount;
    }

    @PreDestroy
    public void shutdown() {
        log.info("애플리케이션 종료. 모든 SSE 연결을 종료합니다. 연결 수: {}", emitters.size());

        Map<Long, SseEmitter> snapshot;
        // 동기화 블록을 사용하여 맵에 대한 동시 접근 제어
        synchronized (emitters) {
            // 스냅샷 복사 후 순회하여 동시성 이슈 방지
            snapshot = new HashMap<>(emitters);
            emitters.clear(); // 먼저 clear하여 새 연결 방지
        }

        snapshot.forEach((memberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(SseEventType.SHUTDOWN.getEventName()).data("server shutdown"));
                emitter.complete();
            } catch (IOException e) {
                log.warn("SSE 종료 메시지 전송 실패 - memberId: {}", memberId);
            }
        });
    }

    private void setupCallbacks(Long memberId, SseEmitter emitter) {
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료 - memberId: {}", memberId);
            removeEmitter(memberId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃 - memberId: {}", memberId);
            removeEmitter(memberId);
        });

        emitter.onError(e -> {
            log.warn("SSE 에러 - memberId: {}, error: {}", memberId, e.getMessage());
            removeEmitter(memberId);
        });
    }

    private void sendConnectEvent(Long memberId) {
        sendToClient(memberId, SseEventType.CONNECT.getEventName(), Map.of(
                "message", "connected",
                "timestamp", System.currentTimeMillis()
        ));
    }

    private void removeEmitter(Long memberId) {
        emitters.remove(memberId);
    }
}
