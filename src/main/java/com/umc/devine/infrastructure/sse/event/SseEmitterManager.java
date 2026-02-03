package com.umc.devine.infrastructure.sse.event;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class SseEmitterManager {

    // SseEmitter 객체 관리 맵
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

     // 애플리케이션 종료를 위한 플래그 (true가 되면 새로운 SSE 연결을 받지 않음)
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    @Value("${sse.timeout}")
    private long sseTimeout;

    /**
     * 새로운 SSE 연결 생성
     * compute()를 사용하여 원자적으로 기존 연결 종료 및 새 연결 생성
     */
    public SseEmitter create(Long memberId) {
        // 레이스 컨디션 방지 : isShuttingDown 가 true이면 새로운 연결 요청을 받지 않음
        if (isShuttingDown.get()) {
            log.warn("서버 종료 중 SSE 연결 시도 거부 - memberId: {}", memberId);
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }

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

    /**
     * 애플리케이션 종료 시 모든 SSE 연결 정리
     * Spring 컨테이너가 종료될 때 @PreDestroy 어노테이션에 의해 자동으로 호출됨.
     */
    @PreDestroy
    public void shutdown() {
        // 1. 새로운 연결 요청을 차단하기 위해 플래그를 true로 설정
        isShuttingDown.set(true);
        log.info("애플리케이션 종료 절차 시작. 모든 SSE 연결을 종료합니다. 현재 연결 수: {}", emitters.size());

        // 2. 현재 활성화된 모든 Emitter에 대해 종료 이벤트를 전송
        emitters.forEach((memberId, emitter) -> {
            try {
                // 클라이언트에게 서버가 종료됨을 알리는 이벤트를 전송.
                emitter.send(SseEmitter.event()
                        .name(SseEventType.SHUTDOWN.getEventName())
                        .data("server shutdown"));
                // Emitter 연결을 정상적으로 완료 처리.
                emitter.complete();
            } catch (IOException e) {
                log.warn("SSE 종료 메시지 전송 실패 - memberId: {}", memberId);
            }
        });

        // 3. 모든 Emitter 객체를 맵에서 제거
        emitters.clear();
        log.info("모든 SSE 연결이 성공적으로 종료되었습니다.");
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
