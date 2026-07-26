package com.umc.devine.admin.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public class AdminProjectReqDTO {

    @Builder
    @Schema(description = "프로젝트 노출 상태 변경 요청")
    public record UpdateVisibilityReq(
            @Schema(description = "변경할 노출 상태 (true=노출, false=비노출)", example = "false")
            @NotNull(message = "노출 상태는 필수입니다.")
            Boolean visible
    ) {}
}
