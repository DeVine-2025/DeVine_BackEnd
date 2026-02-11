package com.umc.devine.infrastructure.fastapi.dto;

import lombok.Builder;

import java.util.List;

public class FastApiReqDto {

    @Builder
    public record ReportGenerationReq(
            Long mainReportId,
            Long detailReportId,
            String gitUrl,
            String callbackUrl,
            String embeddingCallbackUrl,
            String githubToken,
            List<String> techstacks
    ) {}

    @Builder
    public record ReportGenerationSyncReq(
            Long mainReportId,
            Long detailReportId,
            String gitUrl,
            String githubToken,
            String embeddingCallbackUrl,
            List<String> techstacks
    ) {}

    @Builder
    public record ProjectEmbeddingReq(
            String text
    ) {}
}
