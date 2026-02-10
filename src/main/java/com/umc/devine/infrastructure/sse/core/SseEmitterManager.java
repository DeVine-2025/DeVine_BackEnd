package com.umc.devine.infrastructure.sse.core;

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

    private static final long IMMEDIATE_TIMEOUT = 0L;

    // SseEmitter 객체 관리 맵
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 애플리케이션 종료를 위한 플래그 (true가 되면 새로운 SSE 연결을 받지 않음)
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    @Value("${sse.timeout}")
    private long sseTimeout;

    /**
     * 새로운 SSE 연결 생성
     * 기존 연결이 있으면 새 연결로 교체 후 기존 연결 종료
     * (순서 중요: 새 emitter 등록 -> 콜백 설정 -> 기존 emitter 종료)
     */
    public SseEmitter create(Long memberId) {
        // 레이스 컨디션 방지: isShuttingDown이 true이면 새로운 연결 요청을 받지 않음
        if (isShuttingDown.get()) {
            log.warn("서버 종료 중 SSE 연결 시도 거부 - memberId: {}", memberId);
            SseEmitter emitter = new SseEmitter(IMMEDIATE_TIMEOUT);
            emitter.complete();
            return emitter;
        }

        // 1. 기존 emitter 백업
        SseEmitter oldEmitter = emitters.get(memberId);

        // 2. 새 emitter 생성 및 등록 (기존 emitter의 onCompletion보다 먼저 등록)
        SseEmitter newEmitter = new SseEmitter(sseTimeout);
        emitters.put(memberId, newEmitter);

        // 3. 새 emitter에 콜백 설정
        setupCallbacks(memberId, newEmitter);

        // 4. 기존 emitter 종료 (onCompletion이 호출되어도 이미 새 emitter로 덮어씀)
        if (oldEmitter != null) {
            try {
                oldEmitter.complete();
            } catch (Exception e) {
                log.debug("기존 SSE 연결 종료 중 예외 (무시) - memberId: {}", memberId);
            }
            log.debug("기존 SSE 연결 종료 - memberId: {}", memberId);
        }

        // 5. 연결 성공 이벤트 전송
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
            removeEmitterIfMatch(memberId, emitter);
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
            removeEmitterIfMatch(memberId, emitter);
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
     * ConcurrentHashMap.forEach를 사용하여 동시성 안전하게 순회
     */
    public void sendHeartbeatToAll() {
        emitters.forEach((memberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventType.HEARTBEAT.getEventName())
                        .data(Map.of("timestamp", System.currentTimeMillis())));
            } catch (IOException e) {
                log.debug("하트비트 전송 실패 - memberId: {}", memberId);
                removeEmitterIfMatch(memberId, emitter);
            }
        });
    }

    /**
     * 모든 연결에 커스텀 이벤트 브로드캐스트
     * ConcurrentHashMap.forEach를 사용하여 동시성 안전하게 순회
     * @return 전송 성공한 연결 수
     */
    public int broadcast(String eventName, Object data) {
        int[] successCount = {0};
        int totalCount = emitters.size();

        emitters.forEach((memberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                successCount[0]++;
            } catch (IOException e) {
                log.debug("브로드캐스트 전송 실패 - memberId: {}", memberId);
                removeEmitterIfMatch(memberId, emitter);
            }
        });

        log.info("브로드캐스트 완료 - eventName: {}, 성공: {}/{}", eventName, successCount[0], totalCount);
        return successCount[0];
    }

    /**
     * 애플리케이션 종료 시 모든 SSE 연결 정리
     * Spring 컨테이너가 종료될 때 @PreDestroy 어노테이션에 의해 자동으로 호출됨
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

    /**
     * SSE Emitter 콜백 설정
     * 정확한 emitter 인스턴스만 제거하여 동시 연결 시 새 연결이 제거되는 것을 방지
     */
    private void setupCallbacks(Long memberId, SseEmitter emitter) {
        Runnable cleanup = () -> removeEmitterIfMatch(memberId, emitter); // 공통 정리 로직: 정확한 인스턴스만 제거

        emitter.onCompletion(() -> {
            log.debug("SSE 연결 완료 - memberId: {}", memberId);
            cleanup.run();
        });

        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃 - memberId: {}", memberId);
            cleanup.run();
        });

        emitter.onError(e -> {
            log.warn("SSE 에러 - memberId: {}, error: {}", memberId, e.getMessage());
            cleanup.run();
        });
    }

    private void sendConnectEvent(Long memberId) {
        sendToClient(memberId, SseEventType.CONNECT.getEventName(), Map.of(
                "message", "connected",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 정확한 emitter 인스턴스가 일치할 때만 제거
     * 새 연결로 교체된 경우 기존 연결의 콜백이 새 연결을 제거하는 것을 방지
     */
    private void removeEmitterIfMatch(Long memberId, SseEmitter emitter) {
        emitters.remove(memberId, emitter);
    }
}
