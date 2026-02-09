package com.umc.devine.infrastructure.fastapi.dto;

import lombok.Builder;

public class FastApiReqDto {

    @Builder
    public record ReportGenerationReq(
            Long mainReportId,
            Long detailReportId,
            String gitUrl,
            String callbackUrl,
            String embeddingCallbackUrl,
            String githubToken
    ) {}

    @Builder
    public record ReportGenerationSyncReq(
            Long mainReportId,
            Long detailReportId,
            String gitUrl,
            String githubToken,
            String embeddingCallbackUrl
    ) {}

    @Builder
    public record ProjectEmbeddingReq(
            String text
    ) {}
}
