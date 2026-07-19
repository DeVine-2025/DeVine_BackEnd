package com.umc.devine.admin.auth.controller;

import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuthControllerTest extends ControllerIntegrationTestSupport {

    private Authentication adminAuth() {
        AdminPrincipal principal = AdminPrincipal.builder()
                .clerkId("admin_clerk_1")
                .email("admin@devine.com")
                .name("관리자")
                .level(AdminLevel.ADMIN)
                .build();
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Authentication nonAdminAuth() {
        ClerkPrincipal principal = new ClerkPrincipal("user_clerk_1", "user@devine.com", "유저", null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList());
    }

    @Test
    @DisplayName("관리자는 /admin/v1/auth/me에서 자신의 정보(권한 레벨 포함)를 조회한다")
    void me_success_for_admin() throws Exception {
        mockMvc.perform(get("/admin/v1/auth/me")
                        .with(authentication(adminAuth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.clerkId").value("admin_clerk_1"))
                .andExpect(jsonPath("$.result.email").value("admin@devine.com"))
                .andExpect(jsonPath("$.result.level").value("ADMIN"));
    }

    @Test
    @DisplayName("ROLE_ADMIN이 없는 사용자는 403으로 거절된다")
    void me_forbidden_for_non_admin() throws Exception {
        mockMvc.perform(get("/admin/v1/auth/me")
                        .with(authentication(nonAdminAuth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 접근하면 401로 거절된다")
    void me_unauthorized_without_authentication() throws Exception {
        mockMvc.perform(get("/admin/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
