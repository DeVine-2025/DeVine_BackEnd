package com.umc.devine.domain.report.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.devine.domain.report.enums.ReportVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

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
            @Schema(description = "메인 리포트 ID", example = "1")
            @NotNull(message = "메인 리포트 ID는 필수입니다.")
            Long mainReportId,

            @Schema(description = "상세 리포트 ID", example = "2")
            @NotNull(message = "상세 리포트 ID는 필수입니다.")
            Long detailReportId,

            @Schema(description = "처리 상태", example = "SUCCESS")
            @NotNull(message = "상태는 필수입니다.")
            CallbackStatus status,

            @Schema(description = "리포트 내용 (SUCCESS 시, { main: {...}, detail: {...} } 형식)")
            JsonNode content,

            @Schema(description = "에러 메시지 (FAILED 시)")
            String errorMessage,

            @Schema(description = "분석된 기술 스택 목록")
            @Size(max = 50, message = "기술 스택은 최대 50개까지 가능합니다.")
            List<String> techstacks
    ) {}

    public enum CallbackStatus {
        SUCCESS, FAILED
    }
}
