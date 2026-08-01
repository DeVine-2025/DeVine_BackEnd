package com.umc.devine.domain.maintenance.event;

import com.umc.devine.domain.maintenance.dto.MaintenanceState;

/**
 * 점검 모드가 OFF→ON으로 전환될 때 발행되는 이벤트.
 *
 * <p>같은 프로세스 안의 리스너가 이를 받아 기존 실시간 연결(SSE 등)을 정리한다.
 * realtime은 별개 프로세스이므로, realtime 인스턴스는 자신의 {@code refresh()} 폴링이
 * 전환을 감지하는 시점(최대 10초 지연)에 이 이벤트를 발행받아 로컬 연결을 끊는다.
 */
public record MaintenanceEnabledEvent(MaintenanceState state) {
}
