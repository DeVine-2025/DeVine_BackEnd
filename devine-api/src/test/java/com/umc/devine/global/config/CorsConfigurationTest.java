package com.umc.devine.global.config;

import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CORS 동작 회귀 가드.
 *
 * <p>점검 모드 필터를 Security 앞에 두려면 CorsFilter도 그보다 앞으로 빼야 한다.
 * 그 순서 변경이 기존 CORS 동작을 깨뜨리지 않는지 지키기 위한 테스트다.
 * 특히 <b>인증 실패 응답에도 CORS 헤더가 붙는지</b>가 핵심 — 이 성질이 깨지면
 * 브라우저는 에러 본문을 읽지 못하고 네트워크 실패로만 보게 된다.
 */
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class CorsConfigurationTest extends ControllerIntegrationTestSupport {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "https://evil.example.com";

    @Test
    @DisplayName("허용된 오리진의 preflight 요청이 CORS 헤더와 함께 통과한다")
    void preflight_allowedOrigin_succeeds() throws Exception {
        mockMvc.perform(options("/api/v1/projects")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("허용되지 않은 오리진의 preflight 요청은 거부된다")
    void preflight_disallowedOrigin_rejected() throws Exception {
        mockMvc.perform(options("/api/v1/projects")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("인증이 필요한 경로에 토큰 없이 접근하면 401이지만 CORS 헤더는 붙는다")
    void unauthenticated_401_stillCarriesCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/cors-probe-protected-path")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("SSE용 Last-Event-ID 헤더가 노출 헤더로 유지된다")
    void exposedHeaders_preserved() throws Exception {
        mockMvc.perform(options("/api/v1/projects")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Last-Event-ID"));
    }
}
