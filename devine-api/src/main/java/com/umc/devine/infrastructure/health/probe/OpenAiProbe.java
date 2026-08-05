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
 * OpenAI API 점검.
 * 모델 목록 조회는 토큰을 생성하지 않아 과금되지 않으면서 키 유효성과 응답성을 확인할 수 있다.
 */
@Component
public class OpenAiProbe implements IntegrationProbe {

    private static final String OPENAI_MODELS_URL = "https://api.openai.com/v1/models";

    private final RestClient healthCheckRestClient;
    private final ProbeExecutor probeExecutor;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    public OpenAiProbe(
            @Qualifier("healthCheckRestClient") RestClient healthCheckRestClient,
            ProbeExecutor probeExecutor) {
        this.healthCheckRestClient = healthCheckRestClient;
        this.probeExecutor = probeExecutor;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.OPENAI;
    }

    @Override
    public ProbeResult probe() {
        if (!StringUtils.hasText(openAiApiKey)) {
            return probeExecutor.missingConfig();
        }

        return probeExecutor.execute(() -> healthCheckRestClient.get()
                .uri(OPENAI_MODELS_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
                .retrieve()
                .toBodilessEntity());
    }
}
