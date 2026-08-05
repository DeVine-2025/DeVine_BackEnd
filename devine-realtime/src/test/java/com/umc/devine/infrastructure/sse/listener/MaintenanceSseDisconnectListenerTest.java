package com.umc.devine.infrastructure.sse.listener;

import com.umc.devine.domain.maintenance.dto.MaintenanceState;
import com.umc.devine.domain.maintenance.event.MaintenanceEnabledEvent;
import com.umc.devine.infrastructure.sse.core.SseEmitterManager;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 점검 모드 전환 이벤트가 발행되면 realtime의 기존 SSE 연결이 모두 끊기는지 검증한다.
 */
class MaintenanceSseDisconnectListenerTest extends IntegrationTestSupport {

    @Autowired
    private SseEmitterManager sseEmitterManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("MaintenanceEnabledEvent가 발행되면 모든 SSE 연결이 종료된다")
    void onMaintenanceEnabled_disconnectsAllEmitters() {
        // given - SSE 연결 2개
        sseEmitterManager.create(1L);
        sseEmitterManager.create(2L);
        assertThat(sseEmitterManager.getConnectionCount()).isEqualTo(2);

        // when - 점검 전환 이벤트 발행
        MaintenanceState state = new MaintenanceState(true, "점검 중", LocalDateTime.of(2026, 7, 27, 18, 0));
        eventPublisher.publishEvent(new MaintenanceEnabledEvent(state));

        // then - 모든 연결이 정리됨
        assertThat(sseEmitterManager.getConnectionCount()).isZero();
    }
}
