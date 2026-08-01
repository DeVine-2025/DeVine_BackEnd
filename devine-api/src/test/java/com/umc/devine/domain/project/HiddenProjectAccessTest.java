package com.umc.devine.domain.project;

import com.umc.devine.admin.project.service.command.ProjectVisibilityCommandService;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
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

/**
 * 비노출 프로젝트가 작성자 본인 외의 경로로 새어나가지 않는지 검증한다.
 * 작성자 본인이 볼 수 있는 경로는 넓히되, 제3자 노출과 제재 우회는 막는 것이 이 기능의 핵심 제약이다.
 */
class HiddenProjectAccessTest extends IntegrationTestSupport {

    @Autowired
    private ProjectVisibilityCommandService projectVisibilityCommandService;

    @Autowired
    private MatchingCommandService matchingCommandService;

    @Autowired
    private MemberQueryService memberQueryService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectRequirementMemberRepository requirementMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member owner;
    private Member developer;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = memberRepository.save(Member.builder()
                .clerkId("clerk_hidden_owner")
                .name("작성자")
                .nickname("hiddenowner")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        developer = memberRepository.save(Member.builder()
                .clerkId("clerk_hidden_dev")
                .name("개발자")
                .nickname("hiddendev")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        category = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.ECOMMERCE)
                .build());
    }

    private Project createRecruitingProject() {
        Project project = projectRepository.save(Project.builder()
                .name("신고당한 프로젝트")
                .content("원문 내용입니다.")
                .status(ProjectStatus.RECRUITING)
                .projectField(ProjectField.WEB)
                .mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE)
                .location("온라인")
                .recruitmentDeadline(LocalDate.now().plusDays(30))
                .category(category)
                .member(owner)
                .build());

        requirementMemberRepository.save(ProjectRequirementMember.builder()
                .project(project)
                .part(ProjectPart.BACKEND)
                .requirementNum(2)
                .currentCount(0)
                .build());

        return project;
    }

    private Project createHiddenProject() {
        Project project = createRecruitingProject();
        projectVisibilityCommandService.hideForModeration(project.getId(), null);
        return project;
    }

    @Nested
    @DisplayName("지원/제안 차단")
    class MatchingBlockedTest {

        @Test
        @DisplayName("비노출 프로젝트에는 새로 지원할 수 없다")
        void applyToHiddenProject() {
            // given
            Project project = createHiddenProject();

            // when & then
            assertThatThrownBy(() ->
                    matchingCommandService.applyToProject(developer, project.getId(), ProjectPart.BACKEND))
                    .isInstanceOf(MatchingException.class);
        }

        @Test
        @DisplayName("비노출 프로젝트에는 개발자에게 제안할 수 없다")
        void proposeOnHiddenProject() {
            // given
            Project project = createHiddenProject();

            // when & then
            assertThatThrownBy(() ->
                    matchingCommandService.proposeToMember(
                            owner, developer.getNickname(), project.getId(), ProjectPart.BACKEND, "함께해요"))
                    .isInstanceOf(MatchingException.class);
        }

        @Test
        @DisplayName("노출 중인 프로젝트에는 정상적으로 지원할 수 있다")
        void applyToVisibleProject() {
            // given
            Project project = createRecruitingProject();

            // when & then
            assertThat(matchingCommandService.applyToProject(developer, project.getId(), ProjectPart.BACKEND))
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("공개 프로필 노출 차단")
    class PublicProfileTest {

        @Test
        @DisplayName("비노출 프로젝트는 공개 프로필의 프로젝트 목록에 나오지 않는다")
        void publicProfileExcludesHidden() {
            // given
            createHiddenProject();

            // when: 프로필 주인이 아니라 제3자(비회원 포함)가 보는 경로
            ProjectResDTO.MyProjectsRes result = memberQueryService.findProjectsByNickname(
                    owner.getNickname(), List.of(ProjectStatus.RECRUITING), PageRequest.of(0, 10));

            // then
            assertThat(result.projects().getContent()).isEmpty();
        }

        @Test
        @DisplayName("노출 중인 프로젝트는 공개 프로필에 정상적으로 나온다")
        void publicProfileIncludesVisible() {
            // given
            createRecruitingProject();

            // when
            ProjectResDTO.MyProjectsRes result = memberQueryService.findProjectsByNickname(
                    owner.getNickname(), List.of(ProjectStatus.RECRUITING), PageRequest.of(0, 10));

            // then
            assertThat(result.projects().getContent()).hasSize(1);
            assertThat(result.projects().getContent().get(0).visible()).isTrue();
        }
    }
}
