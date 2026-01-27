package com.umc.devine.infrastructure.fast.dto;

import com.umc.devine.domain.report.enums.ReportType;
import lombok.Builder;

public class FastApiReqDto {

    @Builder
    public record ReportGenerationReq(
            Long reportId,
            String gitUrl,
            ReportType reportType,
            String callbackUrl
    ) {}
}
