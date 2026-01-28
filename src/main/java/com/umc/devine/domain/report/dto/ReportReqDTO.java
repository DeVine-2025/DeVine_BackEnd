package com.umc.devine.domain.report.dto;

import com.umc.devine.domain.report.enums.ReportVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class ReportReqDTO {
    @Builder
    public record UpdateVisibilityReq(
            @Schema(description = "공개 범위", example = "PUBLIC")
            @NotNull(message = "공개 범위는 필수입니다.")
            ReportVisibility visibility
    ) {}

    @Builder
    public record CreateReportReq(
            @Schema(description = "Git 저장소 ID", example = "1")
            @NotNull(message = "Git 저장소 ID는 필수입니다.")
            Long gitRepoId
    ) {}

    @Builder
    public record CallbackReq(
            @Schema(description = "리포트 ID", example = "1")
            @NotNull(message = "리포트 ID는 필수입니다.")
            Long reportId,

            @Schema(description = "처리 상태", example = "SUCCESS")
            @NotNull(message = "상태는 필수입니다.")
            CallbackStatus status,

            @Schema(description = "리포트 내용 (SUCCESS 시)")
            String content,

            @Schema(description = "에러 메시지 (FAILED 시)")
            String errorMessage
    ) {}

    public enum CallbackStatus {
        SUCCESS, FAILED
    }
}
