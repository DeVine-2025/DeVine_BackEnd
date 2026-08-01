package com.umc.devine.domain.maintenance.dto;

import com.umc.devine.domain.maintenance.entity.MaintenanceSetting;

import java.time.LocalDateTime;

/**
 * 점검 모드 상태의 불변 스냅샷. 요청마다 DB를 조회하지 않도록 메모리에 캐시되는 값이다.
 */
public record MaintenanceState(
        boolean enabled,
        String message,
        LocalDateTime estimatedEndAt
) {

    public static MaintenanceState from(MaintenanceSetting setting) {
        return new MaintenanceState(
                setting.isEnabled(),
                setting.getMessage(),
                setting.getEstimatedEndAt()
        );
    }
}
