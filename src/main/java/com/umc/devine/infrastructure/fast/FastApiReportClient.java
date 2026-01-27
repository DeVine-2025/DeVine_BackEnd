package com.umc.devine.infrastructure.fast;

import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.event.ReportCreatedEvent;
import com.umc.devine.infrastructure.fast.dto.FastApiReqDto;
import com.umc.devine.infrastructure.fast.dto.FastApiResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class FastApiReportClient {

    private final RestClient fastApiRestClient;

    @Value("${app.callback.base-url:http://localhost:8080}")
    private String callbackBaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReportCreated(ReportCreatedEvent event) {
        DevReport report = event.getReport();
        requestReportGeneration(report);
    }

    public void requestReportGeneration(DevReport report) {
        FastApiReqDto.ReportGenerationReq request = FastApiReqDto.ReportGenerationReq.builder()
                .reportId(report.getId())
                .gitUrl(report.getGitRepoUrl().getGitUrl())
                .reportType(report.getReportType())
                .callbackUrl(callbackBaseUrl + "/api/v1/reports/callback")
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
                    report.getId(),
                    response != null ? response.status() : "null",
                    response != null ? response.message() : "null");

        } catch (RestClientException e) {
            log.error("FastAPI 호출 실패 - reportId: {}, error: {}", report.getId(), e.getMessage());
        }
    }
}
