package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.support.ControllerIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDate;
import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원 탈퇴 API를 HTTP 계층(보안 필터, 요청 검증, 예외 응답 JSON)까지 확인한다.
 * 테스트 트랜잭션이 커밋되지 않으므로 아웃박스 외부 작업(Clerk·S3)은 실행되지 않는다.
 */
class MemberWithdrawalControllerTest extends ControllerIntegrationTestSupport {

    @Autowired private MemberRepository memberRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private MatchingRepository matchingRepository;
    @Autowired private CategoryRepository categoryRepository;

    private UsernamePasswordAuthenticationToken saveMemberAndAuth(String clerkId, String nickname, MemberStatus status) {
        memberRepository.save(Member.builder()
                .clerkId(clerkId).nickname(nickname)
                .mainType(MemberMainType.DEVELOPER).used(status)
                .build());
        ClerkPrincipal principal = new ClerkPrincipal(clerkId, nickname + "@example.com", nickname, null);
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    @Test
    @DisplayName("로그인하지 않으면 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/members/me/withdrawal-preview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("미리보기 200: 대기 건·생성권·환불 연락 이메일 기본값을 돌려준다")
    void preview() throws Exception {
        var auth = saveMemberAndAuth("clerk_wc_preview", "wcpreview", MemberStatus.ACTIVE);

        mockMvc.perform(get("/api/v1/members/me/withdrawal-preview").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER200_11"))
                .andExpect(jsonPath("$.result.pendingCount").value(0))
                .andExpect(jsonPath("$.result.refundableCredits").value(0))
                .andExpect(jsonPath("$.result.refundContactEmail").value("wcpreview@example.com"));
    }

    @Test
    @DisplayName("confirmed가 없거나 false면 400")
    void confirmedRequired() throws Exception {
        var auth = saveMemberAndAuth("clerk_wc_confirm", "wcconfirm", MemberStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/members/me/withdrawal").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/members/me/withdrawal").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmed\": false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("T2: 대기 건이 있으면 409와 result.pendingCount")
    void pendingConflict() throws Exception {
        var auth = saveMemberAndAuth("clerk_wc_pending", "wcpending", MemberStatus.ACTIVE);
        Member developer = memberRepository.findByClerkId("clerk_wc_pending").orElseThrow();
        Member pm = memberRepository.save(Member.builder().clerkId("clerk_wc_pm").nickname("wcpm")
                .mainType(MemberMainType.PM).used(MemberStatus.ACTIVE).build());
        Category category = categoryRepository.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow();
        Project project = projectRepository.save(Project.builder()
                .name("프로젝트").content("소개").status(ProjectStatus.RECRUITING)
                .projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(10)).category(category).member(pm)
                .build());
        matchingRepository.save(Matching.builder().project(project).member(developer)
                .matchingType(MatchingType.APPLY).status(MatchingStatus.PENDING).part(ProjectPart.BACKEND).build());

        mockMvc.perform(post("/api/v1/members/me/withdrawal").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmed\": true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MEMBER409_3"))
                .andExpect(jsonPath("$.result.pendingCount").value(1));
    }

    @Test
    @DisplayName("T1·T9: 탈퇴 200 뒤 같은 토큰으로 다른 API를 부르면 막힌다(403 AUTH403_1)")
    void withdrawThenBlocked() throws Exception {
        var auth = saveMemberAndAuth("clerk_wc_done", "wcdone", MemberStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/members/me/withdrawal").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmed\": true, \"reason\": \"OTHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MEMBER200_13"))
                .andExpect(jsonPath("$.result.withdrawn").value(true))
                .andExpect(jsonPath("$.result.refundRequested").value(false));

        mockMvc.perform(get("/api/v1/members/me").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH403_1"));
        mockMvc.perform(get("/api/v1/members/me/withdrawal-preview").with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("정지 회원은 탈퇴 API만 쓸 수 있고 다른 API는 지금처럼 막힌다")
    void suspendedMemberOnlyWithdrawalApis() throws Exception {
        var auth = saveMemberAndAuth("clerk_wc_susp", "wcsusp", MemberStatus.SUSPENDED);

        mockMvc.perform(get("/api/v1/members/me/withdrawal-preview").with(authentication(auth)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/members/me").with(authentication(auth)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/members/me/withdrawal").with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"confirmed\": true}"))
                .andExpect(status().isOk());
    }
}
