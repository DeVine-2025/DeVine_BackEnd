package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectQueryServiceTest extends IntegrationTestSupport {

    @Autowired
    private ProjectQueryService projectQueryService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProjectRequirementMemberRepository requirementMemberRepository;

    private Member pmMember;
    private Category ecommerceCategory;
    private Category healthcareCategory;

    @BeforeEach
    void setUp() {
        pmMember = memberRepository.save(Member.builder()
                .clerkId("clerk_pm_query")
                .name("PM쿼리")
                .nickname("pmquery")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        ecommerceCategory = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.ECOMMERCE)
                .build());

        healthcareCategory = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.HEALTHCARE)
                .build());
    }

    private Project createProject(String name, ProjectField field, Category category, ProjectStatus status) {
        return projectRepository.save(Project.builder()
                .name(name)
                .content(name + " 내용")
                .status(status)
                .projectField(field)
                .mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE)
                .location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30))
                .category(category)
                .member(pmMember)
                .build());
    }

    @Nested
    @DisplayName("프로젝트 상세 조회")
    class GetProjectDetailTest {

        @Test
        @DisplayName("프로젝트 상세 조회에 성공한다")
        void getProjectDetail_success() {
            // given
            Project project = createProject("상세조회 프로젝트", ProjectField.WEB,
                    ecommerceCategory, ProjectStatus.RECRUITING);

            // when
            ProjectResDTO.UpdateProjectRes result = projectQueryService.getProjectDetail(project.getId());

            // then
            assertThat(result.projectId()).isEqualTo(project.getId());
            assertThat(result.title()).isEqualTo("상세조회 프로젝트");
            assertThat(result.projectField()).isEqualTo(ProjectField.WEB);
            assertThat(result.category()).isEqualTo(CategoryGenre.ECOMMERCE);
            assertThat(result.creatorNickname()).isEqualTo("pmquery");
        }

        @Test
        @DisplayName("삭제된 프로젝트 조회 시 예외가 발생한다")
        void getProjectDetail_deletedProject() {
            // given
            Project project = createProject("삭제 프로젝트", ProjectField.WEB,
                    ecommerceCategory, ProjectStatus.DELETED);

            // when & then
            assertThatThrownBy(() -> projectQueryService.getProjectDetail(project.getId()))
                    .isInstanceOf(ProjectException.class);
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트 조회 시 예외가 발생한다")
        void getProjectDetail_notFound() {
            // when & then
            assertThatThrownBy(() -> projectQueryService.getProjectDetail(999999L))
                    .isInstanceOf(ProjectException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트 검색")
    class SearchProjectsTest {

        @BeforeEach
        void setUp() {
            createProject("웹 프로젝트 1", ProjectField.WEB,
                    ecommerceCategory, ProjectStatus.RECRUITING);
            createProject("웹 프로젝트 2", ProjectField.WEB,
                    healthcareCategory, ProjectStatus.RECRUITING);
            createProject("모바일 프로젝트", ProjectField.MOBILE,
                    ecommerceCategory, ProjectStatus.RECRUITING);
            createProject("삭제된 프로젝트", ProjectField.WEB,
                    ecommerceCategory, ProjectStatus.DELETED);
        }

        @Test
        @DisplayName("필터 없이 전체 프로젝트를 검색한다 (삭제 프로젝트 제외)")
        void searchProjects_noFilter() {
            // given
            ProjectReqDTO.SearchProjectReq request = ProjectReqDTO.SearchProjectReq.builder()
                    .page(1)
                    .size(10)
                    .build();

            // when
            ProjectResDTO.SearchProjectsRes result = projectQueryService.searchProjects(request);

            // then
            assertThat(result.projects().getContent()).hasSize(3);
        }

        @Test
        @DisplayName("프로젝트 분야로 필터링한다")
        void searchProjects_byField() {
            // given
            ProjectReqDTO.SearchProjectReq request = ProjectReqDTO.SearchProjectReq.builder()
                    .projectFields(List.of(ProjectField.WEB))
                    .page(1)
                    .size(10)
                    .build();

            // when
            ProjectResDTO.SearchProjectsRes result = projectQueryService.searchProjects(request);

            // then
            assertThat(result.projects().getContent()).hasSize(2);
            assertThat(result.projects().getContent())
                    .allMatch(p -> p.projectField() == ProjectField.WEB);
        }

        @Test
        @DisplayName("카테고리로 필터링한다")
        void searchProjects_byCategory() {
            // given
            ProjectReqDTO.SearchProjectReq request = ProjectReqDTO.SearchProjectReq.builder()
                    .categories(List.of(CategoryGenre.ECOMMERCE))
                    .page(1)
                    .size(10)
                    .build();

            // when
            ProjectResDTO.SearchProjectsRes result = projectQueryService.searchProjects(request);

            // then
            assertThat(result.projects().getContent()).hasSize(2);
            assertThat(result.projects().getContent())
                    .allMatch(p -> p.category() == CategoryGenre.ECOMMERCE);
        }

        @Test
        @DisplayName("복수 필터를 동시에 적용한다")
        void searchProjects_multipleFilters() {
            // given
            ProjectReqDTO.SearchProjectReq request = ProjectReqDTO.SearchProjectReq.builder()
                    .projectFields(List.of(ProjectField.WEB))
                    .categories(List.of(CategoryGenre.ECOMMERCE))
                    .page(1)
                    .size(10)
                    .build();

            // when
            ProjectResDTO.SearchProjectsRes result = projectQueryService.searchProjects(request);

            // then
            assertThat(result.projects().getContent()).hasSize(1);
            ProjectResDTO.ProjectSummary summary = result.projects().getContent().get(0);
            assertThat(summary.projectField()).isEqualTo(ProjectField.WEB);
            assertThat(summary.category()).isEqualTo(CategoryGenre.ECOMMERCE);
        }

        @Test
        @DisplayName("페이지네이션이 올바르게 동작한다")
        void searchProjects_pagination() {
            // given
            ProjectReqDTO.SearchProjectReq request = ProjectReqDTO.SearchProjectReq.builder()
                    .page(1)
                    .size(2)
                    .build();

            // when
            ProjectResDTO.SearchProjectsRes result = projectQueryService.searchProjects(request);

            // then
            assertThat(result.projects().getContent()).hasSize(2);
            assertThat(result.projects().getTotalElements()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("주간 베스트 프로젝트")
    class WeeklyBestProjectsTest {

        @Test
        @DisplayName("주간 베스트 프로젝트를 최대 4개 반환한다")
        void getWeeklyBestProjects_success() {
            // given
            for (int i = 0; i < 5; i++) {
                createProject("프로젝트 " + i, ProjectField.WEB,
                        ecommerceCategory, ProjectStatus.RECRUITING);
            }

            // when
            ProjectResDTO.WeeklyBestProjectsRes result = projectQueryService.getWeeklyBestProjects();

            // then
            assertThat(result.projects()).hasSizeLessThanOrEqualTo(4);
        }

        @Test
        @DisplayName("삭제된 프로젝트는 주간 베스트에 포함되지 않는다")
        void getWeeklyBestProjects_excludeDeleted() {
            // given
            createProject("정상 프로젝트", ProjectField.WEB,
                    ecommerceCategory, ProjectStatus.RECRUITING);
            createProject("삭제된 프로젝트", ProjectField.WEB,
                    ecommerceCategory, ProjectStatus.DELETED);

            // when
            ProjectResDTO.WeeklyBestProjectsRes result = projectQueryService.getWeeklyBestProjects();

            // then
            assertThat(result.projects()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("내 프로젝트 조회")
    class MyProjectsTest {

        @Test
        @DisplayName("PM의 모집 중 프로젝트를 조회한다")
        void getMyProjects_pmRecruiting() {
            // given
            createProject("모집 중 1", ProjectField.WEB,
                    ecommerceCategory, ProjectStatus.RECRUITING);
            createProject("모집 중 2", ProjectField.MOBILE,
                    ecommerceCategory, ProjectStatus.RECRUITING);
            createProject("진행 중", ProjectField.WEB,
                    ecommerceCategory, ProjectStatus.IN_PROGRESS);

            // when
            ProjectResDTO.MyProjectsRes result = projectQueryService.getMyProjects(
                    pmMember, List.of(ProjectStatus.RECRUITING), PageRequest.of(0, 10));

            // then
            assertThat(result.projects().getContent()).hasSize(2);
        }

        @Test
        @DisplayName("프로젝트가 없으면 빈 결과를 반환한다")
        void getMyProjects_empty() {
            // when
            ProjectResDTO.MyProjectsRes result = projectQueryService.getMyProjects(
                    pmMember, List.of(ProjectStatus.RECRUITING), PageRequest.of(0, 10));

            // then
            assertThat(result.projects().getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("추천 프로젝트 미리보기")
    class RecommendedProjectsPreviewTest {

        @Test
        @DisplayName("임베딩이 없는 회원은 빈 결과를 반환한다")
        void getRecommendedProjectsPreview_noEmbedding() {
            // given
            Member devMember = memberRepository.save(Member.builder()
                    .clerkId("clerk_dev_recommend")
                    .name("추천개발자")
                    .nickname("devrecommend")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            ProjectReqDTO.RecommendProjectsPreviewReq request =
                    ProjectReqDTO.RecommendProjectsPreviewReq.builder().limit(6).build();

            // when
            ProjectResDTO.RecommendedProjectsRes result =
                    projectQueryService.getRecommendedProjectsPreview(devMember, request);

            // then
            assertThat(result.projects()).isEmpty();
            assertThat(result.count()).isEqualTo(0);
        }
    }
}
