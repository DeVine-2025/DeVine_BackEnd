package com.umc.devine.infrastructure.health.probe;

import com.umc.devine.admin.integration.enums.IntegrationType;
import com.umc.devine.infrastructure.health.IntegrationProbe;
import com.umc.devine.infrastructure.health.ProbeExecutor;
import com.umc.devine.infrastructure.health.ProbeResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * FastAPI AI 워커 점검.
 * AI 컨테이너가 이미 노출하고 있는 /health를 그대로 사용한다
 * (docker-compose healthcheck가 동일 경로를 사용).
 */
@Component
public class FastApiProbe implements IntegrationProbe {

    private static final String HEALTH_PATH = "/health";

    private final RestClient healthCheckRestClient;
    private final ProbeExecutor probeExecutor;

    @Value("${fastapi.report.base-url:}")
    private String fastApiBaseUrl;

    public FastApiProbe(
            @Qualifier("healthCheckRestClient") RestClient healthCheckRestClient,
            ProbeExecutor probeExecutor) {
        this.healthCheckRestClient = healthCheckRestClient;
        this.probeExecutor = probeExecutor;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.FASTAPI_AI;
    }

    @Override
    public ProbeResult probe() {
        if (!StringUtils.hasText(fastApiBaseUrl)) {
            return probeExecutor.missingConfig();
        }

        String healthUrl = trimTrailingSlash(fastApiBaseUrl) + HEALTH_PATH;

        return probeExecutor.execute(() -> healthCheckRestClient.get()
                .uri(healthUrl)
                .retrieve()
                .toBodilessEntity());
    }

    private String trimTrailingSlash(String uri) {
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }
}
