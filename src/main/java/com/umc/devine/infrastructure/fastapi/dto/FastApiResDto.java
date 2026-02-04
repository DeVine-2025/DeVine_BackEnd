package com.umc.devine.infrastructure.fastapi.dto;

import lombok.Builder;

public class FastApiResDto {

    @Builder
    public record ReportGenerationRes(
            Long mainReportId,
            Long detailReportId,
            String status,
            String message
    ) {}
}
