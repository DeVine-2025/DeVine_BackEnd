package com.umc.devine.admin.maintenance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.maintenance.dto.MaintenanceState;
import com.umc.devine.domain.maintenance.service.MaintenanceModeService;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private MaintenanceModeService maintenanceModeService;

    @Autowired
    private ObjectMapper objectMapper;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        // 서비스 캐시는 싱글턴이라 테스트 롤백을 따라가지 않는다. DB(초기 OFF)로 동기화한다.
        maintenanceModeService.refresh();

        ClerkPrincipal principal = new ClerkPrincipal("clerk_admin", "admin@example.com", "관리자", null);
        auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    @Test
    @DisplayName("GET - 현재 점검 모드 상태를 조회한다")
    void getState_returnsCurrentState() throws Exception {
        mockMvc.perform(get("/admin/v1/maintenance").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.enabled").value(false));
    }

    @Test
    @DisplayName("PUT - 점검 모드를 켜면 상태가 반영되고 서비스 캐시에도 적용된다")
    void update_enable() throws Exception {
        Map<String, Object> body = Map.of(
                "enabled", true,
                "message", "긴급 점검 중입니다.",
                "estimatedEndAt", "2026-07-25T18:00:00"
        );

        mockMvc.perform(put("/admin/v1/maintenance")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.enabled").value(true))
                .andExpect(jsonPath("$.result.message").value("긴급 점검 중입니다."));

        MaintenanceState state = maintenanceModeService.getState();
        assertThat(state.enabled()).isTrue();
        assertThat(state.message()).isEqualTo("긴급 점검 중입니다.");
    }

    @Test
    @DisplayName("PUT - 점검 모드를 끄면 안내 메시지도 함께 비워진다")
    void update_disable() throws Exception {
        maintenanceModeService.update(true, "점검 중", null);

        Map<String, Object> body = Map.of("enabled", false);

        mockMvc.perform(put("/admin/v1/maintenance")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.enabled").value(false))
                .andExpect(jsonPath("$.result.message").doesNotExist());

        assertThat(maintenanceModeService.getState().enabled()).isFalse();
    }

    @Test
    @DisplayName("PUT - enabled 필드가 없으면 400을 반환한다")
    void update_missingEnabled_returns400() throws Exception {
        Map<String, Object> body = Map.of("message", "메시지만 있음");

        mockMvc.perform(put("/admin/v1/maintenance")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("인증 없이 접근하면 401을 반환한다")
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/admin/v1/maintenance"))
                .andExpect(status().isUnauthorized());
    }
}
