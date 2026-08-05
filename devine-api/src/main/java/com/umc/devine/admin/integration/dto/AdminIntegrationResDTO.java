package com.umc.devine.admin.integration.dto;

import com.umc.devine.admin.integration.enums.IntegrationStatus;
import com.umc.devine.admin.integration.enums.IntegrationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class AdminIntegrationResDTO {

    @Schema(description = "외부 연동 상태 목록")
    public record HealthSummaryDTO(
            @Schema(description = "가장 오래된 항목의 확인 시각 (한 번도 점검하지 않았으면 null)",
                    example = "2026-07-28T14:03:00", nullable = true)
            LocalDateTime checkedAt,

            @Schema(description = "연동 항목별 상태")
            List<IntegrationHealthDTO> integrations
    ) {}

    @Schema(description = "외부 연동 단건 상태")
    public record IntegrationHealthDTO(
            @Schema(description = "연동 식별자", example = "GITHUB_API")
            IntegrationType type,

            @Schema(description = "연동 표시명", example = "GitHub API")
            String name,

            @Schema(description = "상태", example = "NORMAL")
            IntegrationStatus status,

            @Schema(description = "상태 한글 라벨", example = "정상")
            String statusLabel,

            @Schema(description = "응답 시간(ms). 응답을 받지 못했으면 null", example = "142", nullable = true)
            Long responseTimeMs,

            @Schema(description = "최근 확인 시각", example = "2026-07-28T14:03:00")
            LocalDateTime checkedAt,

            @Schema(description = "실패 사유. 정상/지연이면 null", example = "HTTP 401", nullable = true)
            String errorMessage
    ) {}
}
