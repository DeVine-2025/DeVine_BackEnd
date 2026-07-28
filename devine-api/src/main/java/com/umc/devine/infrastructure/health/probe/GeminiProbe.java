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
 * Gemini API 점검.
 * ListModels는 토큰을 생성하지 않아 과금되지 않으면서 키 유효성과 응답성을 확인할 수 있다.
 * 실제 생성(generateContent)이 가능한지까지는 보장하지 않는다.
 * <p>
 * 키를 쿼리 파라미터(?key=) 대신 x-goog-api-key 헤더로 보내는 이유:
 * 쿼리스트링에 담으면 구글 액세스 로그와 중간 프록시에 키가 그대로 남고,
 * 예외 메시지에 URL이 실릴 여지도 생긴다(error_message는 관리자 API로 그대로 노출된다).
 * 나머지 프로브도 전부 헤더 인증이라 방식도 일치한다.
 */
@Component
public class GeminiProbe implements IntegrationProbe {

    private static final String GEMINI_MODELS_URL =
            "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1";

    private static final String GEMINI_API_KEY_HEADER = "x-goog-api-key";

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

        return probeExecutor.execute(() -> healthCheckRestClient.get()
                .uri(GEMINI_MODELS_URL)
                .header(GEMINI_API_KEY_HEADER, geminiApiKey)
                .retrieve()
                .toBodilessEntity());
    }
}
