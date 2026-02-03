package com.umc.devine.infrastructure.fastapi;

import com.umc.devine.domain.report.event.ReportCreatedEvent;
import com.umc.devine.domain.report.service.command.ReportCommandService;
import com.umc.devine.global.external.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiReqDto;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class FastApiReportClient {

    private final RestClient fastApiRestClient;
    private final ReportCommandService reportCommandService;
    private final ClerkApiClient clerkApiClient;

    public FastApiReportClient(
            @Qualifier("fastApiRestClient") RestClient fastApiRestClient,
            ReportCommandService reportCommandService,
            ClerkApiClient clerkApiClient) {
        this.fastApiRestClient = fastApiRestClient;
        this.reportCommandService = reportCommandService;
        this.clerkApiClient = clerkApiClient;
    }

    @Value("${app.callback.base-url:http://localhost:8080}")
    private String callbackBaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportCreated(ReportCreatedEvent event) {
        requestReportGeneration(event);
    }

    public void requestReportGeneration(ReportCreatedEvent event) {
        String githubToken;
        try {
            githubToken = clerkApiClient.getGitHubAccessToken(event.getClerkId());
        } catch (Exception e) {
            log.error("GitHub 토큰 조회 실패 - reportId: {}, error: {}", event.getReportId(), e.getMessage());
            reportCommandService.deleteReport(event.getReportId());
            return;
        }

        FastApiReqDto.ReportGenerationReq request = FastApiReqDto.ReportGenerationReq.builder()
                .reportId(event.getReportId())
                .gitUrl(event.getGitUrl())
                .reportType(event.getReportType())
                .callbackUrl(callbackBaseUrl + "/api/v1/reports/callback")
                .githubToken(githubToken)
                .build();

        log.info("FastAPI 리포트 생성 요청 - reportId: {}, gitUrl: {}, reportType: {}",
                request.reportId(), request.gitUrl(), request.reportType());

        try {
            FastApiResDto.ReportGenerationRes response = fastApiRestClient.post()
                    .uri("/api/v1/reports/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FastApiResDto.ReportGenerationRes.class);

            log.info("FastAPI 응답 - reportId: {}, status: {}, message: {}",
                    event.getReportId(),
                    response != null ? response.status() : "null",
                    response != null ? response.message() : "null");

        } catch (RestClientException e) {
            log.error("FastAPI 호출 실패 - reportId: {}, error: {}", event.getReportId(), e.getMessage());
            reportCommandService.deleteReport(event.getReportId());
        }
    }
}
