package com.umc.devine.admin.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.umc.devine.domain.maintenance.dto.MaintenanceState;

import java.time.LocalDateTime;

/**
 * 점검 모드 상태 응답. 점검이 꺼져 있으면 message·estimatedEndAt는 응답에서 생략된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaintenanceResDTO(
        boolean enabled,
        String message,
        LocalDateTime estimatedEndAt
) {

    public static MaintenanceResDTO from(MaintenanceState state) {
        return new MaintenanceResDTO(state.enabled(), state.message(), state.estimatedEndAt());
    }
}
