package com.umc.devine.admin.project.service.query;

import com.umc.devine.admin.project.dto.AdminProjectReqDTO;
import com.umc.devine.admin.project.dto.AdminProjectResDTO;
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
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AdminProjectQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private AdminProjectQueryService adminProjectQueryService;

    @Autowired
    private ProjectVisibilityCommandService projectVisibilityCommandService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member owner;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = memberRepository.save(Member.builder()
                .clerkId("clerk_admin_list_owner")
                .name("작성자")
                .nickname("list_owner")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        category = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.ECOMMERCE)
                .build());
    }

    private Project createProject(String name, ProjectStatus status) {
        return projectRepository.save(Project.builder()
                .name(name)
                .content(name + " 내용")
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

    private AdminProjectReqDTO.SearchReq searchReq(Boolean visible) {
        return AdminProjectReqDTO.SearchReq.builder().visible(visible).build();
    }

    @Test
    @DisplayName("프론트에 필요한 ID, 제목, 작성자, 등록일, 노출상태를 반환한다")
    void getProjectList_returnsRequiredFields() {
        // given
        Project project = createProject("쇼핑몰 프로젝트", ProjectStatus.RECRUITING);

        // when
        PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> result =
                adminProjectQueryService.getProjectList(searchReq(null));

        // then
        assertThat(result.getContent()).hasSize(1);
        AdminProjectResDTO.ProjectSummaryDTO row = result.getContent().get(0);
        assertThat(row.projectId()).isEqualTo(project.getId());
        assertThat(row.title()).isEqualTo("쇼핑몰 프로젝트");
        assertThat(row.authorNickname()).isEqualTo("list_owner");
        assertThat(row.createdAt()).isNotNull();
        assertThat(row.visible()).isTrue();
    }

    @Test
    @DisplayName("필터를 지정하지 않으면 노출/비노출을 모두 조회한다")
    void getProjectList_noFilter() {
        // given
        createProject("노출 프로젝트", ProjectStatus.RECRUITING);
        Project hidden = createProject("비노출 프로젝트", ProjectStatus.RECRUITING);
        projectVisibilityCommandService.hideForModeration(hidden.getId(), null);

        // when
        PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> result =
                adminProjectQueryService.getProjectList(searchReq(null));

        // then
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("visible=false면 비노출 프로젝트만 조회한다")
    void getProjectList_hiddenOnly() {
        // given
        createProject("노출 프로젝트", ProjectStatus.RECRUITING);
        Project hidden = createProject("비노출 프로젝트", ProjectStatus.RECRUITING);
        projectVisibilityCommandService.hideForModeration(hidden.getId(), null);

        // when
        PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> result =
                adminProjectQueryService.getProjectList(searchReq(false));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("비노출 프로젝트");
        assertThat(result.getContent().get(0).visible()).isFalse();
    }

    @Test
    @DisplayName("visible=true면 노출 중인 프로젝트만 조회한다")
    void getProjectList_visibleOnly() {
        // given
        createProject("노출 프로젝트", ProjectStatus.RECRUITING);
        Project hidden = createProject("비노출 프로젝트", ProjectStatus.RECRUITING);
        projectVisibilityCommandService.hideForModeration(hidden.getId(), null);

        // when
        PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> result =
                adminProjectQueryService.getProjectList(searchReq(true));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("노출 프로젝트");
    }

    @Test
    @DisplayName("삭제된 프로젝트는 목록에서 제외된다")
    void getProjectList_excludeDeleted() {
        // given
        createProject("정상 프로젝트", ProjectStatus.RECRUITING);
        createProject("삭제된 프로젝트", ProjectStatus.DELETED);

        // when
        PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> result =
                adminProjectQueryService.getProjectList(searchReq(null));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("정상 프로젝트");
    }

    @Test
    @DisplayName("등록일 최신순으로 정렬된다")
    void getProjectList_orderByCreatedAtDesc() {
        // given
        createProject("먼저 등록", ProjectStatus.RECRUITING);
        createProject("나중 등록", ProjectStatus.RECRUITING);

        // when
        PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> result =
                adminProjectQueryService.getProjectList(searchReq(null));

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).createdAt())
                .isAfterOrEqualTo(result.getContent().get(1).createdAt());
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 목록을 반환한다")
    void getProjectList_empty() {
        // when
        PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> result =
                adminProjectQueryService.getProjectList(searchReq(null));

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
