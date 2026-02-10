package com.umc.devine.domain.member.controller;

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
import com.umc.devine.global.auth.ClerkPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MemberControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private Authentication testAuth;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_test_123")
                .name("테스트")
                .nickname("testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        // MockMvc에서 사용할 인증 객체 생성
        ClerkPrincipal principal = new ClerkPrincipal("clerk_test_123", "test@example.com", "테스트", null);
        testAuth = new UsernamePasswordAuthenticationToken(principal, null, java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("닉네임 기반 프로필 조회 성공")
    void getMemberByNickname_success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/members/testuser")
                        .with(authentication(testAuth))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.nickname").value("testuser"));
    }

    @Test
    @DisplayName("닉네임 중복 여부 확인 성공")
    void checkNicknameDuplicate_success() throws Exception {
        // when & then - permitAll 엔드포인트이므로 인증 불필요
        mockMvc.perform(get("/api/v1/members/nickname/check")
                        .param("nickname", "newuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.isDuplicate").value(false));
    }

    @Test
    @DisplayName("개발자 검색 성공")
    void searchDevelopers_success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/members/search")
                        .with(authentication(testAuth))
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.content").isArray());
    }

    @Test
    @DisplayName("개발자 추천 프리뷰 조회 성공")
    void getRecommendDevelopersPreview_success() throws Exception {
        mockMvc.perform(get("/api/v1/members/recommend/preview")
                        .with(authentication(testAuth))
                        .param("limit", "4")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }

    @Test
    @DisplayName("이용약관 조회 성공")
    void getTerms_success() throws Exception {
        // when & then - permitAll 엔드포인트이므로 인증 불필요
        mockMvc.perform(get("/api/v1/members/terms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }
}