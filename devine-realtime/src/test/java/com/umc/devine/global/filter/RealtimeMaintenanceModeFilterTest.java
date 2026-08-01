package com.umc.devine.global.filter;

import com.umc.devine.domain.maintenance.service.MaintenanceModeService;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * devine-realtime에도 점검 모드가 적용되는지 검증한다.
 *
 * <p>realtime은 별도 애플리케이션이라 점검 모드를 켜도 채팅/SSE가 계속 살아 있으면
 * "일반 유저 요청 차단"이 절반만 이뤄진다.
 *
 * <p>한계: 필터는 WebSocket <b>핸드셰이크(HTTP 업그레이드)</b>만 볼 수 있으므로
 * 점검 전환 시점에 이미 열려 있던 연결은 끊기지 않는다. 신규 연결만 차단된다.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class RealtimeMaintenanceModeFilterTest extends IntegrationTestSupport {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MaintenanceModeService maintenanceModeService;

    private void enableMaintenance() {
        maintenanceModeService.update(true, "서버 점검 중입니다.", LocalDateTime.of(2026, 7, 21, 18, 0));
    }

    @AfterEach
    void disableMaintenance() {
        maintenanceModeService.update(false, null, null);
    }

    @Nested
    @DisplayName("점검 모드 ON")
    class WhenEnabled {

        @Test
        @DisplayName("SSE 구독 요청이 503 점검 안내로 차단된다")
        void sse_blockedWith503() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/sse/subscribe"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("MAINTENANCE503_1"))
                    .andExpect(jsonPath("$.result.message").value("서버 점검 중입니다."));
        }

        @Test
        @DisplayName("WebSocket 핸드셰이크가 503으로 차단된다 - 신규 연결만 해당")
        void webSocketHandshake_blockedWith503() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/ws/chat"))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("503 응답에도 CORS 헤더가 붙는다")
        void blockedResponse_carriesCorsHeaders() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/sse/subscribe").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
        }

        @Test
        @DisplayName("actuator 헬스체크는 통과한다")
        void actuator_passesThrough() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("점검 모드 OFF")
    class WhenDisabled {

        @Test
        @DisplayName("SSE 구독은 평소대로 인증을 요구한다")
        void sse_stillRequiresAuthentication() throws Exception {
            mockMvc.perform(get("/sse/subscribe"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
