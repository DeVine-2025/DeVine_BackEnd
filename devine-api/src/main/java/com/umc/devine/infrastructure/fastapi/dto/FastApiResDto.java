package com.umc.devine.infrastructure.fastapi.dto;

import lombok.Builder;

import java.util.List;

public class FastApiResDto {

    @Builder
    public record ReportGenerationRes(
            Long mainReportId,
            Long detailReportId,
            String status,
            String message
    ) {}

    @Builder
    public record EmbeddingRes(
            List<Double> vector,
            Integer dimension
    ) {}
}
