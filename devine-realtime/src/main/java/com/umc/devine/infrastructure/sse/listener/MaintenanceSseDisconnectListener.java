package com.umc.devine.infrastructure.sse.listener;

import com.umc.devine.domain.maintenance.event.MaintenanceEnabledEvent;
import com.umc.devine.infrastructure.sse.core.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 점검 모드가 켜지면 기존 SSE 연결을 끊는다.
 *
 * <p>신규 연결은 점검 필터가 이미 막지만, 이미 열려 있던 SSE 스트림은 필터를 거치지 않아
 * 계속 살아 있다. 이 리스너가 그 연결들을 정리한다. realtime은 별개 프로세스이므로
 * 이벤트는 이 프로세스의 {@code refresh()} 폴링이 전환을 감지하는 시점에 도착한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceSseDisconnectListener {

    private final SseEmitterManager sseEmitterManager;

    @EventListener
    public void onMaintenanceEnabled(MaintenanceEnabledEvent event) {
        sseEmitterManager.disconnectAllForMaintenance();
    }
}
