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
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Gemini API 점검.
 * ListModels는 토큰을 생성하지 않아 과금되지 않으면서 키 유효성과 응답성을 확인할 수 있다.
 * 실제 생성(generateContent)이 가능한지까지는 보장하지 않는다.
 */
@Component
public class GeminiProbe implements IntegrationProbe {

    private static final String GEMINI_MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final RestClient healthCheckRestClient;
    private final ProbeExecutor probeExecutor;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    public GeminiProbe(
            @Qualifier("healthCheckRestClient") RestClient healthCheckRestClient,
            ProbeExecutor probeExecutor) {
        this.healthCheckRestClient = healthCheckRestClient;
        this.probeExecutor = probeExecutor;
    }

    @Override
    public IntegrationType getType() {
        return IntegrationType.GEMINI;
    }

    @Override
    public ProbeResult probe() {
        if (!StringUtils.hasText(geminiApiKey)) {
            return probeExecutor.missingConfig();
        }

        String modelsUrl = UriComponentsBuilder.fromUriString(GEMINI_MODELS_URL)
                .queryParam("key", geminiApiKey)
                .queryParam("pageSize", 1)
                .toUriString();

        return probeExecutor.execute(() -> healthCheckRestClient.get()
                .uri(modelsUrl)
                .retrieve()
                .toBodilessEntity());
    }
}
