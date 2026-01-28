package com.umc.devine.infrastructure.fastapi.dto;

import lombok.Builder;

public class FastApiResDto {

    @Builder
    public record ReportGenerationRes(
            Long reportId,
            String status,
            String message
    ) {}
}
