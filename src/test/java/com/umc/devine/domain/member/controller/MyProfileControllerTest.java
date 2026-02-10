package com.umc.devine.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.Terms;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.repository.TermsRepository;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.global.auth.ClerkPrincipal;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MyProfileControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TechstackRepository techstackRepository;

    @Autowired
    private DevTechstackRepository devTechstackRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Member testMember;
    private Authentication testAuth;
    private Terms requiredTerms;
    private Category testCategory;
    private Techstack testTechstack;

    @BeforeEach
    void setUp() {
        requiredTerms = termsRepository.save(Terms.builder()
                .title("서비스 이용약관")
                .content("약관 내용")
                .required(true)
                .build());

        testCategory = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.HEALTHCARE)
                .build());

        testTechstack = techstackRepository.save(Techstack.builder()
                .name(TechName.JAVA)
                .genre(TechGenre.LANGUAGE)
                .build());

        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_test_123")
                .name("테스트")
                .nickname("testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        ClerkPrincipal principal = new ClerkPrincipal("clerk_test_123", "test@example.com", "테스트", null);
        testAuth = new UsernamePasswordAuthenticationToken(principal, null, java.util.Collections.emptyList());
    }

    @Nested
    @DisplayName("내 프로필 조회")
    class GetMemberProfileTest {

        @Test
        @DisplayName("내 프로필 조회 성공")
        void getMemberProfile_success() throws Exception {
            mockMvc.perform(get("/api/v1/members/me")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.member.nickname").value("testuser"));
        }
    }

    @Nested
    @DisplayName("내 프로필 수정")
    class PatchMemberTest {

        @Test
        @DisplayName("내 프로필 수정 성공")
        void patchMember_success() throws Exception {
            MemberReqDTO.UpdateMemberDTO dto = MemberReqDTO.UpdateMemberDTO.builder()
                    .nickname("updateduser")
                    .body("자기소개 수정")
                    .address("서울시 강남구")
                    .build();

            mockMvc.perform(patch("/api/v1/members/me")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.member.nickname").value("updateduser"));
        }
    }

    @Nested
    @DisplayName("내 기술 스택 조회")
    class GetMyTechstacksTest {

        @Test
        @DisplayName("내 기술 스택 조회 성공")
        void getMyTechstacks_success() throws Exception {
            // given
            DevTechstack devTechstack = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            mockMvc.perform(get("/api/v1/members/me/techstacks")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.techstacks").isArray());
        }
    }

    @Nested
    @DisplayName("내 기술 스택 추가")
    class AddMyTechstacksTest {

        @Test
        @DisplayName("내 기술 스택 추가 성공")
        void addMyTechstacks_success() throws Exception {
            MemberReqDTO.AddTechstackDTO dto = MemberReqDTO.AddTechstackDTO.builder()
                    .techstackIds(new Long[] {testTechstack.getId()})
                    .build();

            mockMvc.perform(post("/api/v1/members/me/techstacks")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    @Nested
    @DisplayName("내 기술 스택 삭제")
    class RemoveMyTechstacksTest {

        @Test
        @DisplayName("내 기술 스택 삭제 성공")
        void removeMyTechstacks_success() throws Exception {
            // given
            DevTechstack devTechstack = DevTechstack.builder()
                    .member(testMember)
                    .techstack(testTechstack)
                    .source(TechstackSource.MANUAL)
                    .build();
            devTechstackRepository.save(devTechstack);

            MemberReqDTO.RemoveTechstackDTO dto = MemberReqDTO.RemoveTechstackDTO.builder()
                    .techstackIds(new Long[] {testTechstack.getId()})
                    .build();

            mockMvc.perform(delete("/api/v1/members/me/techstacks")
                            .with(authentication(testAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }

    @Nested
    @DisplayName("회원가입")
    class SignupTest {

        @Test
        @DisplayName("회원가입 성공")
        void signup_success() throws Exception {
            // given - 새로운 사용자로 인증 설정
            ClerkPrincipal newPrincipal = new ClerkPrincipal("clerk_new_user", "new@example.com", "새사용자", null);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(newPrincipal, null, java.util.Collections.emptyList());

            MemberReqDTO.SignupDTO dto = MemberReqDTO.SignupDTO.builder()
                    .agreements(List.of(
                            MemberReqDTO.AgreementDTO.builder()
                                    .termsId(requiredTerms.getId())
                                    .agreed(true)
                                    .build()
                    ))
                    .nickname("newuser")
                    .mainType(MemberMainType.DEVELOPER)
                    .categoryIds(List.of(testCategory.getId()))
                    .build();

            mockMvc.perform(post("/api/v1/members/signup")
                            .with(authentication(newAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.nickname").value("newuser"));
        }
    }
}
