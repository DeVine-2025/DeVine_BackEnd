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
 * Clerk JWKS 엔드포인트 점검.
 * Spring Security가 모든 JWT를 검증할 때 의존하는 곳이라 Clerk API와 별도로 점검한다.
 */
@Component
public class ClerkJwksProbe implements IntegrationProbe {

    private static final String JWKS_PATH = "/.well-known/jwks.json";

    private final RestClient healthCheckRestClient;
    private final ProbeExecutor probeExecutor;

    @Value("${clerk.issuer-uri:}")
    private String clerkIssuerUri;

    public ClerkJwksProbe(
            @Qualifier("healthCheckRestClient") RestClient healthCheckRestClient,
            ProbeExecutor probeExecutor) {
        this.healthCheckRestClient = healthCheckRestClient;
        this.probeExecutor = probeExecutor;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.CLERK_JWKS;
    }

    @Override
    public ProbeResult probe() {
        if (!StringUtils.hasText(clerkIssuerUri)) {
            return probeExecutor.missingConfig();
        }

        String jwksUrl = trimTrailingSlash(clerkIssuerUri) + JWKS_PATH;

        return probeExecutor.execute(() -> healthCheckRestClient.get()
                .uri(jwksUrl)
                .retrieve()
                .toBodilessEntity());
    }

    private String trimTrailingSlash(String uri) {
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }
}
