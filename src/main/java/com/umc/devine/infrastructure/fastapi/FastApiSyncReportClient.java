package com.umc.devine.infrastructure.fastapi;

import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorCode;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiReqDto;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import com.umc.devine.infrastructure.github.GitHubApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class FastApiSyncReportClient {

    private final RestClient fastApiSyncRestClient;
    private final ClerkApiClient clerkApiClient;
    private final GitHubApiClient gitHubApiClient;

    @Value("${fastapi.callback.base-url:http://localhost:8080}")
    private String callbackBaseUrl;

    public FastApiSyncReportClient(
            @Qualifier("fastApiSyncRestClient") RestClient fastApiSyncRestClient,
            ClerkApiClient clerkApiClient,
            GitHubApiClient gitHubApiClient
    ) {
        this.fastApiSyncRestClient = fastApiSyncRestClient;
        this.clerkApiClient = clerkApiClient;
        this.gitHubApiClient = gitHubApiClient;
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

        List<String> allTechstacks = Arrays.stream(TechName.values())
                .map(TechName::name)
                .toList();

        List<String> authorEmails;
        try {
            authorEmails = gitHubApiClient.getAllAuthorEmails(githubToken);
            log.info("GitHub 이메일 조회 성공 - mainReportId: {}, emails: {}", mainReport.getId(), authorEmails);
        } catch (Exception e) {
            log.warn("GitHub 이메일 조회 실패, 빈 리스트로 진행 - error: {}", e.getMessage());
            authorEmails = Collections.emptyList();
        }

        FastApiReqDto.ReportGenerationSyncReq request = FastApiReqDto.ReportGenerationSyncReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .gitUrl(gitUrl)
                .githubToken(githubToken)
                .embeddingCallbackUrl(callbackBaseUrl + "/api/v1/embeddings/callback")
                .techstacks(allTechstacks)
                .authorEmails(authorEmails)
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
