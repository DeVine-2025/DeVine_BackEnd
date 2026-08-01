package com.umc.devine.admin.integration.controller;

import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;
import com.umc.devine.admin.integration.exception.IntegrationAdminException;
import com.umc.devine.admin.integration.exception.code.IntegrationAdminErrorReason;
import com.umc.devine.admin.integration.exception.code.AdminIntegrationSuccessCode;
import com.umc.devine.admin.integration.service.command.AdminIntegrationCommandService;
import com.umc.devine.admin.integration.service.query.AdminIntegrationQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.ratelimit.SimpleRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/v1/integrations")
public class AdminIntegrationController implements AdminIntegrationControllerDocs {

    private static final String REFRESH_RATE_LIMIT_KEY = "rate-limit:integration-health-refresh";
    private static final int REFRESH_RATE_LIMIT = 6;
    private static final Duration REFRESH_RATE_WINDOW = Duration.ofMinutes(1);

    private final AdminIntegrationQueryService adminIntegrationQueryService;
    private final AdminIntegrationCommandService adminIntegrationCommandService;
    private final SimpleRateLimiter rateLimiter;

    @Override
    @GetMapping("/health")
    public ApiResponse<AdminIntegrationResDTO.HealthSummaryDTO> getIntegrationHealth() {
        return ApiResponse.onSuccess(
                AdminIntegrationSuccessCode.INTEGRATION_HEALTH_FOUND,
                adminIntegrationQueryService.getHealthSnapshot()
        );
    }

    @Override
    @PostMapping("/health/refresh")
    public ApiResponse<AdminIntegrationResDTO.HealthSummaryDTO> refreshIntegrationHealth() {
        // 새로고침 연타로 외부 API를 두들기지 않도록 전역 제한 (관리자 인증 도입 전이라 계정 단위로 나눌 수 없음)
        if (!rateLimiter.isAllowed(REFRESH_RATE_LIMIT_KEY, REFRESH_RATE_LIMIT, REFRESH_RATE_WINDOW)) {
            throw new IntegrationAdminException(IntegrationAdminErrorReason.REFRESH_RATE_LIMIT_EXCEEDED);
        }

        return ApiResponse.onSuccess(
                AdminIntegrationSuccessCode.INTEGRATION_HEALTH_REFRESHED,
                adminIntegrationCommandService.refreshAll()
        );
    }
}
