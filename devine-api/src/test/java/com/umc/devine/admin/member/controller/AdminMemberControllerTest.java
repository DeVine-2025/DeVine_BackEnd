package com.umc.devine.admin.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminMemberControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member target;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        target = memberRepository.save(Member.builder()
                .clerkId("clerk_target")
                .name("유저")
                .nickname("targetuser")
                .mainType(MemberMainType.DEVELOPER)
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

    @Nested
    @DisplayName("유저 목록 조회")
    class GetMemberListTest {

        @Test
        @DisplayName("관리자는 유저 목록을 조회할 수 있다")
        void getMemberList_success() throws Exception {
            mockMvc.perform(get("/admin/v1/member")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content").isArray());
        }
    }

    @Nested
    @DisplayName("유저 상세 조회")
    class GetMemberDetailTest {

        @Test
        @DisplayName("존재하는 닉네임으로 상세를 조회할 수 있다")
        void getMemberDetail_success() throws Exception {
            mockMvc.perform(get("/admin/v1/member/{nickname}", target.getNickname())
                            .with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.nickname").value(target.getNickname()));
        }

        @Test
        @DisplayName("존재하지 않는 닉네임이면 404를 반환한다")
        void getMemberDetail_notFound() throws Exception {
            mockMvc.perform(get("/admin/v1/member/{nickname}", "no-such-nickname")
                            .with(authentication(adminAuth)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("계정 상태 변경")
    class ChangeStatusTest {

        @Test
        @DisplayName("정지 요청 시 상태가 변경된다")
        void changeStatus_suspend_success() throws Exception {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.SUSPEND)
                    .reason("커뮤니티 이용규칙 위반")
                    .build();

            mockMvc.perform(patch("/admin/v1/member/{nickname}/status", target.getNickname())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("SUSPENDED"));
        }

        @Test
        @DisplayName("사유 없이 정지 요청하면 400을 반환한다")
        void changeStatus_suspend_withoutReason() throws Exception {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.SUSPEND)
                    .build();

            mockMvc.perform(patch("/admin/v1/member/{nickname}/status", target.getNickname())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("강제탈퇴 요청 시 PENDING_WITHDRAWAL로 변경된다")
        void changeStatus_forceWithdraw_success() throws Exception {
            AdminMemberReqDTO.ChangeStatusReq request = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.FORCE_WITHDRAW)
                    .reason("중대한 규정 위반")
                    .build();

            mockMvc.perform(patch("/admin/v1/member/{nickname}/status", target.getNickname())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("PENDING_WITHDRAWAL"));
        }

        @Test
        @DisplayName("정지 후 정지해제 요청하면 ACTIVE로 복귀한다")
        void changeStatus_unsuspend_success() throws Exception {
            AdminMemberReqDTO.ChangeStatusReq suspendRequest = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.SUSPEND)
                    .reason("커뮤니티 이용규칙 위반")
                    .build();
            mockMvc.perform(patch("/admin/v1/member/{nickname}/status", target.getNickname())
                    .with(authentication(adminAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(suspendRequest)));

            AdminMemberReqDTO.ChangeStatusReq unsuspendRequest = AdminMemberReqDTO.ChangeStatusReq.builder()
                    .action(MemberStatusAction.UNSUSPEND)
                    .build();

            mockMvc.perform(patch("/admin/v1/member/{nickname}/status", target.getNickname())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(unsuspendRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value("ACTIVE"));
        }
    }
}
