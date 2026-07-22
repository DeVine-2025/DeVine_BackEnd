package com.umc.devine.infrastructure.fastapi.dto;

import com.fasterxml.jackson.databind.JsonNode;
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
    public record ReportGenerationSyncRes(
            Long mainReportId,
            Long detailReportId,
            String status,
            JsonNode content,
            String errorMessage,
            List<String> techstacks
    ) {}

    @Builder
    public record EmbeddingRes(
            List<Double> vector,
            Integer dimension
    ) {}
}
