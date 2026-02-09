package com.umc.devine.infrastructure.fastapi;

import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorCode;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiReqDto;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class FastApiSyncReportClient {

    private final RestClient fastApiSyncRestClient;
    private final ClerkApiClient clerkApiClient;

    public FastApiSyncReportClient(
            @Qualifier("fastApiSyncRestClient") RestClient fastApiSyncRestClient,
            ClerkApiClient clerkApiClient
    ) {
        this.fastApiSyncRestClient = fastApiSyncRestClient;
        this.clerkApiClient = clerkApiClient;
    }

    public FastApiResDto.ReportGenerationSyncRes requestReportGenerationSync(
            DevReport mainReport,
            DevReport detailReport,
            String gitUrl,
            String clerkId
    ) {
        String githubToken;
        try {
            githubToken = clerkApiClient.getGitHubAccessToken(clerkId);
        } catch (Exception e) {
            log.error("GitHub 토큰 조회 실패 - mainReportId: {}, detailReportId: {}, error: {}",
                    mainReport.getId(), detailReport.getId(), e.getMessage());
            throw new ReportException(ReportErrorCode.GITHUB_TOKEN_ERROR);
        }

        FastApiReqDto.ReportGenerationSyncReq request = FastApiReqDto.ReportGenerationSyncReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .gitUrl(gitUrl)
                .githubToken(githubToken)
                .build();

        log.info("FastAPI 동기 리포트 생성 요청 - mainReportId: {}, detailReportId: {}, gitUrl: {}",
                request.mainReportId(), request.detailReportId(), request.gitUrl());

        try {
            FastApiResDto.ReportGenerationSyncRes response = fastApiSyncRestClient.post()
                    .uri("/api/v1/reports/generate/sync")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(FastApiResDto.ReportGenerationSyncRes.class);

            log.info("FastAPI 동기 응답 - mainReportId: {}, detailReportId: {}, status: {}",
                    mainReport.getId(), detailReport.getId(),
                    response != null ? response.status() : "null");

            return response;
        } catch (RestClientException e) {
            log.error("FastAPI 동기 호출 실패 - mainReportId: {}, detailReportId: {}, error: {}",
                    mainReport.getId(), detailReport.getId(), e.getMessage());
            throw new ReportException(ReportErrorCode.FASTAPI_REQUEST_FAILED);
        }
    }
}
