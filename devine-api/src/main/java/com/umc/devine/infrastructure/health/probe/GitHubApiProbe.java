package com.umc.devine.infrastructure.health.probe;

import com.umc.devine.admin.integration.enums.IntegrationType;
import com.umc.devine.infrastructure.health.IntegrationProbe;
import com.umc.devine.infrastructure.health.ProbeExecutor;
import com.umc.devine.infrastructure.health.ProbeResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * GitHub API 점검.
 * /rate_limit은 자체 rate limit을 소모하지 않아 주기 점검에 적합하다.
 * 서비스 토큰이 만료되면 401이 떨어지므로 토큰 유효성도 함께 확인된다.
 */
@Component
public class GitHubApiProbe implements IntegrationProbe {

    private static final String GITHUB_RATE_LIMIT_URL = "https://api.github.com/rate_limit";

    private final RestClient healthCheckRestClient;
    private final ProbeExecutor probeExecutor;

    @Value("${github.service-token:}")
    private String gitHubServiceToken;

    public GitHubApiProbe(
            @Qualifier("healthCheckRestClient") RestClient healthCheckRestClient,
            ProbeExecutor probeExecutor) {
        this.healthCheckRestClient = healthCheckRestClient;
        this.probeExecutor = probeExecutor;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.GITHUB_API;
    }

    @Override
    public ProbeResult probe() {
        if (!StringUtils.hasText(gitHubServiceToken)) {
            return probeExecutor.missingConfig();
        }

        return probeExecutor.execute(() -> healthCheckRestClient.get()
                .uri(GITHUB_RATE_LIMIT_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + gitHubServiceToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .retrieve()
                .toBodilessEntity());
    }
}
