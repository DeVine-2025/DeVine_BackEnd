package com.umc.devine.admin.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.admin.project.dto.AdminProjectReqDTO;
import com.umc.devine.admin.project.service.command.ProjectVisibilityCommandService;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.ProjectRepository;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminProjectControllerTest extends ControllerIntegrationTestSupport {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectVisibilityCommandService projectVisibilityCommandService;

    private Member owner;
    private Category category;
    private Authentication adminAuth;

    @BeforeEach
    void setUp() {
        owner = memberRepository.save(Member.builder()
                .clerkId("clerk_project_owner")
                .name("소유자")
                .nickname("project_owner")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        memberRepository.save(Member.builder()
                .clerkId("clerk_project_admin")
                .name("관리자")
                .nickname("project_admin")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        category = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.ECOMMERCE)
                .build());

        AdminPrincipal principal = AdminPrincipal.builder()
                .clerkId("clerk_project_admin")
                .email("admin@example.com")
                .name("관리자")
                .level(AdminLevel.ADMIN)
                .build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                principal, null, java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Project createProject(ProjectStatus status) {
        return projectRepository.save(Project.builder()
                .name("프로젝트")
                .content("프로젝트 원문 내용입니다.")
                .status(status)
                .projectField(ProjectField.WEB)
                .mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE)
                .location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30))
                .category(category)
                .member(owner)
                .build());
    }

    @Nested
    @DisplayName("관리자 프로젝트 목록 조회")
    class GetProjectListTest {

        @Test
        @DisplayName("프론트에 필요한 필드를 담아 목록을 조회한다")
        void getProjectList_success() throws Exception {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);

            // when & then
            mockMvc.perform(get("/admin/v1/projects")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.content[0].projectId").value(project.getId()))
                    .andExpect(jsonPath("$.result.content[0].title").value("프로젝트"))
                    .andExpect(jsonPath("$.result.content[0].authorNickname").value("project_owner"))
                    .andExpect(jsonPath("$.result.content[0].createdAt").exists())
                    .andExpect(jsonPath("$.result.content[0].visible").value(true));
        }

        @Test
        @DisplayName("visible=false로 비노출 프로젝트만 조회한다")
        void getProjectList_hiddenOnly() throws Exception {
            // given
            createProject(ProjectStatus.RECRUITING);
            Project hidden = createProject(ProjectStatus.RECRUITING);
            projectVisibilityCommandService.hideForModeration(hidden.getId(), null);

            // when & then
            mockMvc.perform(get("/admin/v1/projects")
                            .with(authentication(adminAuth))
                            .param("visible", "false")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content.length()").value(1))
                    .andExpect(jsonPath("$.result.content[0].projectId").value(hidden.getId()))
                    .andExpect(jsonPath("$.result.content[0].visible").value(false));
        }

        @Test
        @DisplayName("결과가 없어도 에러가 아니라 빈 목록을 반환한다")
        void getProjectList_empty() throws Exception {
            mockMvc.perform(get("/admin/v1/projects")
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content").isArray())
                    .andExpect(jsonPath("$.result.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("프로젝트 노출/비노출 전환")
    class UpdateVisibilityTest {

        @Test
        @DisplayName("프로젝트를 비노출로 전환한다")
        void updateVisibility_hide() throws Exception {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);
            AdminProjectReqDTO.UpdateVisibilityReq request = AdminProjectReqDTO.UpdateVisibilityReq.builder()
                    .visible(false)
                    .build();

            // when & then
            mockMvc.perform(patch("/admin/v1/projects/{projectId}/visibility", project.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.visible").value(false))
                    .andExpect(jsonPath("$.result.changed").value(true));

            assertThat(projectRepository.findById(project.getId()).orElseThrow().isHidden()).isTrue();
        }

        @Test
        @DisplayName("이미 동일한 상태로 다시 요청해도 200과 changed=false를 반환한다")
        void updateVisibility_idempotent() throws Exception {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);
            AdminProjectReqDTO.UpdateVisibilityReq request = AdminProjectReqDTO.UpdateVisibilityReq.builder()
                    .visible(false)
                    .build();
            String body = objectMapper.writeValueAsString(request);

            mockMvc.perform(patch("/admin/v1/projects/{projectId}/visibility", project.getId())
                    .with(authentication(adminAuth))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));

            // when & then
            mockMvc.perform(patch("/admin/v1/projects/{projectId}/visibility", project.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.changed").value(false));
        }

        @Test
        @DisplayName("visible이 없으면 400을 반환한다")
        void updateVisibility_missingVisible() throws Exception {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);

            // when & then
            mockMvc.perform(patch("/admin/v1/projects/{projectId}/visibility", project.getId())
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트면 404를 반환한다")
        void updateVisibility_notFound() throws Exception {
            // given
            AdminProjectReqDTO.UpdateVisibilityReq request = AdminProjectReqDTO.UpdateVisibilityReq.builder()
                    .visible(false)
                    .build();

            // when & then
            mockMvc.perform(patch("/admin/v1/projects/{projectId}/visibility", 999999L)
                            .with(authentication(adminAuth))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }
}
