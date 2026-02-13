package com.umc.devine.domain.embedding.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class EmbeddingCallbackDto {

    public enum CallbackStatus {
        SUCCESS, FAILED
    }

    public record ReportEmbeddingCallback(
            @NotNull(message = "detailReportId는 필수입니다")
            Long detailReportId,
            @NotNull(message = "mainReportId는 필수입니다")
            Long mainReportId,
            @NotNull(message = "status는 필수입니다")
            CallbackStatus status,
            List<Double> vector,
            Integer dimension,
            String errorMessage
    ) {}

    public record ProjectEmbeddingCallback(
            @NotNull(message = "projectId는 필수입니다")
            Long projectId,
            @NotNull(message = "status는 필수입니다")
            CallbackStatus status,
            List<Double> vector,
            Integer dimension,
            String errorMessage
    ) {}
}
