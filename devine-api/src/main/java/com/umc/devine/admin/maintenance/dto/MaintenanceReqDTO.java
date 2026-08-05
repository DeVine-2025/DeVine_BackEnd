package com.umc.devine.admin.maintenance.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class MaintenanceReqDTO {

    /**
     * 점검 모드 전환 요청.
     *
     * @param enabled        점검 모드 on/off (필수)
     * @param message        점검 안내 메시지 (선택)
     * @param estimatedEndAt 예상 종료 시각 (선택)
     */
    public record UpdateDTO(
            @NotNull Boolean enabled,
            String message,
            LocalDateTime estimatedEndAt
    ) {
    }
}
