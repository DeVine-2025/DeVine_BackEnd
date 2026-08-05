package com.umc.devine.admin.dashboard.controller;

import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminDashboardControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        memberRepository.save(Member.builder()
                .clerkId("clerk_admin")
                .name("관리자")
                .nickname("admin")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        AdminPrincipal principal = AdminPrincipal.builder()
                .clerkId("clerk_admin")
                .email("admin@example.com")
                .name("관리자")
                .level(AdminLevel.ADMIN)
                .build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("대시보드 지표를 조회한다")
    void getDashboard_success() throws Exception {
        mockMvc.perform(get("/admin/v1/dashboard")
                        .with(authentication(adminAuth))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("ADMINDASHBOARD200_1"))
                .andExpect(jsonPath("$.result.pendingComplaintCount").exists())
                .andExpect(jsonPath("$.result.couponUsageRate").exists())
                .andExpect(jsonPath("$.result.todayPaymentCount").exists());
    }

    @Test
    @DisplayName("인증 없이 호출하면 401을 반환한다")
    void getDashboard_unauthorized() throws Exception {
        mockMvc.perform(get("/admin/v1/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
