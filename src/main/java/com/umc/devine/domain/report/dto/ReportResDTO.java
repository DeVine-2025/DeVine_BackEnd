package com.umc.devine.domain.report.dto;

import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.enums.ReportVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

public class ReportResDTO {

    @Builder
    public record ReportRes(
            @Schema(description = "리포트 ID", example = "1")
            Long reportId,

            @Schema(description = "Git 저장소 ID", example = "1")
            Long gitRepoId,

            @Schema(description = "Git 저장소 URL", example = "https://github.com/user/repo")
            String gitRepoUrl,

            @Schema(description = "리포트 타입", example = "MAIN")
            ReportType reportType,

            @Schema(description = "공개 범위", example = "PUBLIC")
            ReportVisibility visibility,

            @Schema(description = "리포트 내용")
            String content,

            @Schema(description = "에러 메시지")
            String errorMessage,

            @Schema(description = "완료 시간")
            LocalDateTime completedAt,

            @Schema(description = "생성 시간")
            LocalDateTime createdAt
    ) {}

    @Builder
    public record UpdateVisibilityRes(
            @Schema(description = "리포트 ID", example = "1")
            Long reportId,

            @Schema(description = "변경된 공개 범위", example = "PRIVATE")
            ReportVisibility visibility
    ) {}
}
