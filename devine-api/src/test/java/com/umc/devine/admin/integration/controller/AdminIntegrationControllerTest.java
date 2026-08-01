package com.umc.devine.admin.integration.controller;

import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;
import com.umc.devine.admin.integration.entity.ExternalIntegrationHealth;
import com.umc.devine.admin.integration.enums.IntegrationStatus;
import com.umc.devine.admin.integration.enums.IntegrationType;
import com.umc.devine.admin.integration.repository.ExternalIntegrationHealthRepository;
import com.umc.devine.admin.integration.service.command.AdminIntegrationCommandService;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIntegrationControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private ExternalIntegrationHealthRepository externalIntegrationHealthRepository;

    /** 실제 외부 API를 호출하지 않도록 재점검 서비스를 대체한다 */
    @MockitoBean
    private AdminIntegrationCommandService adminIntegrationCommandService;

    /** /admin/** 는 ROLE_ADMIN을 요구하므로 관리자 인증을 붙여 요청한다 */
    protected Authentication adminAuth;

    @BeforeEach
    void setUpAdminAuth() {
        AdminPrincipal principal = AdminPrincipal.builder()
                .clerkId("clerk_admin")
                .email("admin@example.com")
                .name("관리자")
                .level(AdminLevel.ADMIN)
                .build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @AfterEach
    void tearDown() {
        externalIntegrationHealthRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /admin/v1/integrations/health")
    class GetHealth {

        @Test
        @DisplayName("저장된 스냅샷을 반환한다")
        void returnsStoredSnapshot() throws Exception {
            externalIntegrationHealthRepository.save(ExternalIntegrationHealth.builder()
                    .integrationType(IntegrationType.GITHUB_API)
                    .status(IntegrationStatus.NORMAL)
                    .responseTimeMs(142L)
                    .checkedAt(LocalDateTime.now())
                    .build());

            mockMvc.perform(get("/admin/v1/integrations/health").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("ADMININTEGRATION200_1"))
                    .andExpect(jsonPath("$.result.checkedAt").isNotEmpty())
                    .andExpect(jsonPath("$.result.integrations[0].type").value("GITHUB_API"))
                    .andExpect(jsonPath("$.result.integrations[0].name").value("GitHub API"))
                    .andExpect(jsonPath("$.result.integrations[0].status").value("NORMAL"))
                    .andExpect(jsonPath("$.result.integrations[0].statusLabel").value("정상"))
                    .andExpect(jsonPath("$.result.integrations[0].responseTimeMs").value(142));
        }

        @Test
        @DisplayName("확인 불가 항목은 응답 시간이 null이고 사유가 함께 내려간다")
        void returnsUnknownWithReason() throws Exception {
            externalIntegrationHealthRepository.save(ExternalIntegrationHealth.builder()
                    .integrationType(IntegrationType.PORTONE)
                    .status(IntegrationStatus.UNKNOWN)
                    .checkedAt(LocalDateTime.now())
                    .errorMessage("설정값 없음")
                    .build());

            mockMvc.perform(get("/admin/v1/integrations/health").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.integrations[0].status").value("UNKNOWN"))
                    .andExpect(jsonPath("$.result.integrations[0].statusLabel").value("확인 불가"))
                    .andExpect(jsonPath("$.result.integrations[0].responseTimeMs").doesNotExist())
                    .andExpect(jsonPath("$.result.integrations[0].errorMessage").value("설정값 없음"));
        }

        @Test
        @DisplayName("한 번도 점검하지 않았으면 빈 목록을 반환한다")
        void returnsEmptyWhenNeverChecked() throws Exception {
            mockMvc.perform(get("/admin/v1/integrations/health").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.integrations").isEmpty())
                    .andExpect(jsonPath("$.result.checkedAt").doesNotExist());
        }
    }

    @Nested
    @DisplayName("POST /admin/v1/integrations/health/refresh")
    class RefreshHealth {

        private AdminIntegrationResDTO.HealthSummaryDTO stubSummary() {
            return new AdminIntegrationResDTO.HealthSummaryDTO(
                    LocalDateTime.now(),
                    List.of(new AdminIntegrationResDTO.IntegrationHealthDTO(
                            IntegrationType.CLERK_API,
                            IntegrationType.CLERK_API.getDisplayName(),
                            IntegrationStatus.NORMAL,
                            IntegrationStatus.NORMAL.getLabel(),
                            88L,
                            LocalDateTime.now(),
                            null)));
        }

        @Test
        @DisplayName("재점검 결과를 반환한다")
        void returnsRefreshedResult() throws Exception {
            given(adminIntegrationCommandService.refreshAll()).willReturn(stubSummary());

            mockMvc.perform(post("/admin/v1/integrations/health/refresh").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("ADMININTEGRATION200_2"))
                    .andExpect(jsonPath("$.result.integrations[0].type").value("CLERK_API"))
                    .andExpect(jsonPath("$.result.integrations[0].status").value("NORMAL"));
        }

        @Test
        @DisplayName("분당 6회를 초과하면 429를 반환한다")
        void rejectsWhenRateLimitExceeded() throws Exception {
            given(adminIntegrationCommandService.refreshAll()).willReturn(stubSummary());

            for (int i = 0; i < 6; i++) {
                mockMvc.perform(post("/admin/v1/integrations/health/refresh").with(authentication(adminAuth)))
                        .andExpect(status().isOk());
            }

            mockMvc.perform(post("/admin/v1/integrations/health/refresh").with(authentication(adminAuth)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("INTEGRATIONADMIN429_1"));
        }
    }
}
