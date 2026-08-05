package com.umc.devine.admin.project.service.command;

import com.umc.devine.admin.project.dto.AdminProjectResDTO;
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
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectVisibilityCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private ProjectVisibilityCommandService projectVisibilityCommandService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Member owner;
    private Member admin;
    private Category category;

    @BeforeEach
    void setUp() {
        owner = memberRepository.save(Member.builder()
                .clerkId("clerk_visibility_owner")
                .name("소유자")
                .nickname("visibility_owner")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        admin = memberRepository.save(Member.builder()
                .clerkId("clerk_visibility_admin")
                .name("관리자")
                .nickname("visibility_admin")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        category = categoryRepository.save(Category.builder()
                .genre(CategoryGenre.ECOMMERCE)
                .build());
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
    @DisplayName("changeVisibility")
    class ChangeVisibilityTest {

        @Test
        @DisplayName("비노출로 전환하면 hidden이 true가 되고 라이프사이클 상태는 그대로 보존된다")
        void changeVisibility_hide() {
            // given
            Project project = createProject(ProjectStatus.IN_PROGRESS);

            // when
            AdminProjectResDTO.UpdateVisibilityRes result =
                    projectVisibilityCommandService.changeVisibility(project.getId(), false, admin.getClerkId());

            // then
            assertThat(result.visible()).isFalse();
            assertThat(result.changed()).isTrue();

            Project updated = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(updated.isHidden()).isTrue();
            assertThat(updated.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("다시 노출로 되돌리면 비노출 이전의 라이프사이클 상태가 그대로 살아있다")
        void changeVisibility_restore() {
            // given
            Project project = createProject(ProjectStatus.COMPLETED);
            projectVisibilityCommandService.changeVisibility(project.getId(), false, admin.getClerkId());

            // when
            AdminProjectResDTO.UpdateVisibilityRes result =
                    projectVisibilityCommandService.changeVisibility(project.getId(), true, admin.getClerkId());

            // then
            assertThat(result.visible()).isTrue();
            assertThat(result.changed()).isTrue();

            Project updated = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(updated.isHidden()).isFalse();
            assertThat(updated.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
        }

        @Test
        @DisplayName("이미 동일한 노출 상태여도 예외 없이 처리되고 changed는 false다")
        void changeVisibility_idempotent() {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);
            projectVisibilityCommandService.changeVisibility(project.getId(), false, admin.getClerkId());

            // when
            AdminProjectResDTO.UpdateVisibilityRes result =
                    projectVisibilityCommandService.changeVisibility(project.getId(), false, admin.getClerkId());

            // then
            assertThat(result.changed()).isFalse();
            assertThat(result.visible()).isFalse();

            Project updated = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(updated.isHidden()).isTrue();
            // 멱등 호출을 반복해도 원래 상태는 훼손되지 않는다
            assertThat(updated.getStatus()).isEqualTo(ProjectStatus.RECRUITING);
        }

        @Test
        @DisplayName("처리자와 처리시각이 기록된다")
        void changeVisibility_recordsProcessor() {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);

            // when
            projectVisibilityCommandService.changeVisibility(project.getId(), false, admin.getClerkId());

            // then
            Project updated = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(updated.getVisibilityChangedBy().getId()).isEqualTo(admin.getId());
            assertThat(updated.getVisibilityChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("처리자가 없어도(비로그인) 노출 상태는 변경된다")
        void changeVisibility_withoutProcessor() {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);

            // when
            AdminProjectResDTO.UpdateVisibilityRes result =
                    projectVisibilityCommandService.changeVisibility(project.getId(), false, null);

            // then
            assertThat(result.processorMemberId()).isNull();
            assertThat(projectRepository.findById(project.getId()).orElseThrow().isHidden()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트면 예외가 발생한다")
        void changeVisibility_notFound() {
            assertThatThrownBy(() -> projectVisibilityCommandService.changeVisibility(999999L, false, admin.getClerkId()))
                    .isInstanceOf(ProjectException.class);
        }

        @Test
        @DisplayName("이미 삭제된 프로젝트는 노출 전환 대상이 아니라 예외가 발생한다")
        void changeVisibility_deletedProject() {
            // given
            Project project = createProject(ProjectStatus.DELETED);

            // when & then
            assertThatThrownBy(() -> projectVisibilityCommandService.changeVisibility(project.getId(), false, admin.getClerkId()))
                    .isInstanceOf(ProjectException.class);
        }
    }

    @Nested
    @DisplayName("hideForModeration")
    class HideForModerationTest {

        @Test
        @DisplayName("정상 프로젝트를 비노출 처리하고 true를 반환한다")
        void hideForModeration_success() {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);

            // when
            boolean result = projectVisibilityCommandService.hideForModeration(project.getId(), admin.getClerkId());

            // then
            assertThat(result).isTrue();
            assertThat(projectRepository.findById(project.getId()).orElseThrow().isHidden()).isTrue();
        }

        @Test
        @DisplayName("이미 비노출인 프로젝트도 예외 없이 true를 반환한다")
        void hideForModeration_alreadyHidden() {
            // given
            Project project = createProject(ProjectStatus.RECRUITING);
            projectVisibilityCommandService.hideForModeration(project.getId(), admin.getClerkId());

            // when
            boolean result = projectVisibilityCommandService.hideForModeration(project.getId(), admin.getClerkId());

            // then
            assertThat(result).isTrue();
            assertThat(projectRepository.findById(project.getId()).orElseThrow().isHidden()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 프로젝트면 예외 없이 false를 반환한다")
        void hideForModeration_notFound() {
            assertThat(projectVisibilityCommandService.hideForModeration(999999L, admin.getClerkId())).isFalse();
        }

        @Test
        @DisplayName("targetId가 null이어도 예외 없이 false를 반환한다")
        void hideForModeration_nullTargetId() {
            assertThat(projectVisibilityCommandService.hideForModeration(null, admin.getClerkId())).isFalse();
        }

        @Test
        @DisplayName("이미 삭제된 프로젝트면 예외 없이 false를 반환한다")
        void hideForModeration_deletedProject() {
            // given
            Project project = createProject(ProjectStatus.DELETED);

            // when & then
            assertThat(projectVisibilityCommandService.hideForModeration(project.getId(), admin.getClerkId())).isFalse();
        }
    }
}
