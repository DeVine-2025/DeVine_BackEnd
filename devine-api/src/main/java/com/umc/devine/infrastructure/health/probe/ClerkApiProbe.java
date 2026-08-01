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
 * Clerk Backend API 점검.
 * 회원 1건만 조회해 시크릿 키 유효성과 API 응답성을 함께 확인한다.
 */
@Component
public class ClerkApiProbe implements IntegrationProbe {

    private static final String CLERK_USERS_URL = "https://api.clerk.com/v1/users?limit=1";

    private final RestClient healthCheckRestClient;
    private final ProbeExecutor probeExecutor;

    @Value("${clerk.secret-key:}")
    private String clerkSecretKey;

    public ClerkApiProbe(
            @Qualifier("healthCheckRestClient") RestClient healthCheckRestClient,
            ProbeExecutor probeExecutor) {
        this.healthCheckRestClient = healthCheckRestClient;
        this.probeExecutor = probeExecutor;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.CLERK_API;
    }

    @Override
    public ProbeResult probe() {
        if (!StringUtils.hasText(clerkSecretKey)) {
            return probeExecutor.missingConfig();
        }

        return probeExecutor.execute(() -> healthCheckRestClient.get()
                .uri(CLERK_USERS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + clerkSecretKey)
                .retrieve()
                .toBodilessEntity());
    }
}
