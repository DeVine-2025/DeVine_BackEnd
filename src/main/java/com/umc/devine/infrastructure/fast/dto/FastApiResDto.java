package com.umc.devine.infrastructure.fast.dto;

import lombok.Builder;

public class FastApiResDto {

    @Builder
    public record ReportGenerationRes(
            Long reportId,
            String status,
            String message
    ) {}
}
