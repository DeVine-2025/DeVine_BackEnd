package com.umc.devine.global.filter;

import com.umc.devine.domain.maintenance.service.MaintenanceModeService;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 점검 모드 필터의 차단/통과 동작을 검증한다.
 *
 * <p>필터는 Security보다 앞에서 동작하므로 인증 여부와 무관하게 판단해야 하고,
 * 관리자 예외는 role이 아니라 경로(/admin/**)로만 표현된다.
 */
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class MaintenanceModeFilterTest extends ControllerIntegrationTestSupport {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

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
        @DisplayName("일반 API 요청은 503과 점검 안내 본문을 받는다")
        void apiRequest_blockedWith503() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("MAINTENANCE503_1"))
                    .andExpect(jsonPath("$.result.message").value("서버 점검 중입니다."))
                    .andExpect(jsonPath("$.result.estimatedEndAt").exists());
        }

        @Test
        @DisplayName("토큰이 없어도 401이 아니라 503을 받는다 - 필터가 Security보다 앞에 있어야 성립")
        void unauthenticatedRequest_gets503NotUnauthorized() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/api/v1/maintenance-probe-protected-path"))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("503 응답에도 CORS 헤더가 붙어 브라우저가 점검 안내를 읽을 수 있다")
        void blockedResponse_carriesCorsHeaders() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/api/v1/projects").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
        }

        @Test
        @DisplayName("preflight 요청은 점검 중에도 통과한다")
        void preflight_passesThrough() throws Exception {
            enableMaintenance();

            mockMvc.perform(options("/api/v1/projects")
                            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
        }

        @Test
        @DisplayName("한글 안내 메시지가 깨지지 않는다")
        void koreanMessage_notMangled() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("서비스 점검 중입니다."));
        }

        @Test
        @DisplayName("Retry-After 헤더를 종료 예정 시각 기준으로 내려준다")
        void retryAfterHeader_present() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
        }

        @Test
        @DisplayName("관리자 경로는 통과한다 - 점검 중에도 관리자가 접근할 수 있어야 한다")
        void adminPath_passesThrough() throws Exception {
            enableMaintenance();

            // 점검 필터에서 차단되지 않았음을 확인한다.
            // (인증/인가는 뒤의 Security가 담당하므로 503만 아니면 통과로 본다)
            mockMvc.perform(get("/admin/v1/maintenance"))
                    .andExpect(status().is(not(503)));
        }

        @Test
        @DisplayName("actuator 헬스체크는 통과한다 - 점검 중에도 인프라가 서버 상태를 봐야 한다")
        void actuator_passesThrough() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PortOne 웹훅은 통과한다 - 결제 통보 유실을 막기 위함")
        void portOneWebhook_passesThrough() throws Exception {
            enableMaintenance();

            // 서명 검증에서 거부되더라도 점검 필터에서 막히지만 않으면 된다
            mockMvc.perform(post("/api/v1/payments/webhook")
                            .header("Webhook-Id", "test")
                            .header("Webhook-Timestamp", "test")
                            .header("Webhook-Signature", "test")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().is(not(503)));
        }

        @Test
        @DisplayName("개발용 /dev 페이지는 통과한다 - 점검 중 복구 작업에 필요하다")
        void devPage_passesThrough() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/dev/index.html"))
                    .andExpect(status().is(not(503)));
        }

        @Test
        @DisplayName("swagger 문서는 통과한다")
        void swagger_passesThrough() throws Exception {
            enableMaintenance();

            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().is(not(503)));
        }
    }

    @Nested
    @DisplayName("점검 모드 OFF")
    class WhenDisabled {

        @Test
        @DisplayName("일반 API 요청이 정상 처리된다")
        void apiRequest_passesThrough() throws Exception {
            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("인증이 필요한 경로는 평소대로 401을 받는다")
        void protectedPath_stillReturns401() throws Exception {
            mockMvc.perform(get("/api/v1/maintenance-probe-protected-path"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
