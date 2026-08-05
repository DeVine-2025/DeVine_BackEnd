package com.umc.devine.infrastructure.health.probe;

import com.umc.devine.admin.integration.enums.IntegrationType;
import com.umc.devine.infrastructure.health.IntegrationProbe;
import com.umc.devine.infrastructure.health.ProbeExecutor;
import com.umc.devine.infrastructure.health.ProbeResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * PortOne API 점검.
 * API 시크릿으로 액세스 토큰을 발급받아 결제를 건드리지 않고 도달성과 시크릿 유효성을 확인한다.
 * 발급된 토큰은 사용하지 않고 버린다.
 */
@Component
public class PortOneProbe implements IntegrationProbe {

    private static final String LOGIN_PATH = "/login/api-secret";

    private final RestClient healthCheckRestClient;
    private final ProbeExecutor probeExecutor;

    @Value("${portone.base-url:}")
    private String portOneBaseUrl;

    @Value("${portone.api-secret:}")
    private String portOneApiSecret;

    public PortOneProbe(
            @Qualifier("healthCheckRestClient") RestClient healthCheckRestClient,
            ProbeExecutor probeExecutor) {
        this.healthCheckRestClient = healthCheckRestClient;
        this.probeExecutor = probeExecutor;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.PORTONE;
    }

    @Override
    public ProbeResult probe() {
        if (!StringUtils.hasText(portOneBaseUrl) || !StringUtils.hasText(portOneApiSecret)) {
            return probeExecutor.missingConfig();
        }

        String loginUrl = trimTrailingSlash(portOneBaseUrl) + LOGIN_PATH;

        return probeExecutor.execute(() -> healthCheckRestClient.post()
                .uri(loginUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("apiSecret", portOneApiSecret))
                .retrieve()
                .toBodilessEntity());
    }

    private String trimTrailingSlash(String uri) {
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }
}
