package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.support.CoreIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    private Member testMember;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_repo_test")
                .name("레포테스트")
                .nickname("repotest")
                .mainType(MemberMainType.PM)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        testCategory = categoryRepository.save(Category.builder()
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
                .category(testCategory)
                .member(testMember)
                .build());
    }

    private Project createHiddenProject(String name, ProjectStatus status) {
        Project project = createProject(name, status);
        project.changeVisibility(false, testMember, LocalDateTime.now());
        return projectRepository.save(project);
    }

    @Nested
    @DisplayName("findVisibleById")
    class FindVisibleByIdTest {

        @Test
        @DisplayName("삭제되지 않은 프로젝트를 조회한다")
        void findVisibleById_success() {
            // given
            Project project = createProject("정상 프로젝트", ProjectStatus.RECRUITING);

            // when
            Optional<Project> result = projectRepository.findVisibleById(project.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("정상 프로젝트");
        }

        @Test
        @DisplayName("삭제된 프로젝트는 조회되지 않는다")
        void findVisibleById_deleted() {
            // given
            Project project = createProject("삭제된 프로젝트", ProjectStatus.DELETED);

            // when
            Optional<Project> result = projectRepository.findVisibleById(project.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("비노출 처리된 프로젝트는 상태가 모집중이어도 조회되지 않는다")
        void findVisibleById_hidden() {
            // given
            Project project = createHiddenProject("비노출 프로젝트", ProjectStatus.RECRUITING);

            // when
            Optional<Project> result = projectRepository.findVisibleById(project.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findVisibleByIdWithMember")
    class FindVisibleByIdWithMemberTest {

        @Test
        @DisplayName("멤버와 카테고리를 함께 조회한다")
        void findVisibleByIdWithMember_success() {
            // given
            Project project = createProject("페치조인 프로젝트", ProjectStatus.RECRUITING);

            // when
            Optional<Project> result = projectRepository.findVisibleByIdWithMember(project.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getMember().getNickname()).isEqualTo("repotest");
            assertThat(result.get().getCategory().getGenre()).isEqualTo(CategoryGenre.ECOMMERCE);
        }

        @Test
        @DisplayName("비노출 처리된 프로젝트는 조회되지 않는다")
        void findVisibleByIdWithMember_hidden() {
            // given
            Project project = createHiddenProject("비노출 프로젝트", ProjectStatus.RECRUITING);

            // when
            Optional<Project> result = projectRepository.findVisibleByIdWithMember(project.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByMemberAndStatusIn")
    class FindByMemberAndStatusInTest {

        @Test
        @DisplayName("회원의 특정 상태 프로젝트를 페이지네이션으로 조회한다")
        void findByMemberAndStatusIn_success() {
            // given
            createProject("모집 중 1", ProjectStatus.RECRUITING);
            createProject("모집 중 2", ProjectStatus.RECRUITING);
            createProject("진행 중", ProjectStatus.IN_PROGRESS);

            // when
            Page<Project> result = projectRepository.findByMemberAndStatusIn(
                    testMember, List.of(ProjectStatus.RECRUITING), PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allMatch(p -> p.getStatus() == ProjectStatus.RECRUITING);
        }

        @Test
        @DisplayName("복수 상태로 조회한다")
        void findByMemberAndStatusIn_multipleStatuses() {
            // given
            createProject("모집 중", ProjectStatus.RECRUITING);
            createProject("진행 중", ProjectStatus.IN_PROGRESS);
            createProject("완료", ProjectStatus.COMPLETED);

            // when
            Page<Project> result = projectRepository.findByMemberAndStatusIn(
                    testMember,
                    List.of(ProjectStatus.RECRUITING, ProjectStatus.IN_PROGRESS),
                    PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("작성자 본인의 목록이므로 비노출 프로젝트도 포함된다")
        void findByMemberAndStatusIn_includeHidden() {
            // given
            createProject("모집 중", ProjectStatus.RECRUITING);
            createHiddenProject("비노출 모집 중", ProjectStatus.RECRUITING);

            // when
            Page<Project> result = projectRepository.findByMemberAndStatusIn(
                    testMember, List.of(ProjectStatus.RECRUITING), PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findAllByMemberAndStatusIn")
    class FindAllByMemberAndStatusInTest {

        @Test
        @DisplayName("작성자 본인의 목록이므로 비노출 프로젝트도 포함된다")
        void findAllByMemberAndStatusIn_includeHidden() {
            // given
            createProject("모집 중", ProjectStatus.RECRUITING);
            createHiddenProject("비노출 모집 중", ProjectStatus.RECRUITING);

            // when
            List<Project> result = projectRepository.findAllByMemberAndStatusIn(
                    testMember, List.of(ProjectStatus.RECRUITING));

            // then
            assertThat(result).hasSize(2);
            assertThat(result).anyMatch(Project::isHidden);
        }

        @Test
        @DisplayName("삭제된 프로젝트는 상태 목록에 없으므로 조회되지 않는다")
        void findAllByMemberAndStatusIn_excludeDeleted() {
            // given
            createProject("모집 중", ProjectStatus.RECRUITING);
            createProject("삭제됨", ProjectStatus.DELETED);

            // when
            List<Project> result = projectRepository.findAllByMemberAndStatusIn(
                    testMember, List.of(ProjectStatus.RECRUITING));

            // then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByIdWithMemberVisibleTo")
    class FindByIdWithMemberVisibleToTest {

        @Test
        @DisplayName("비노출 프로젝트는 작성자 본인에게는 조회된다")
        void findByIdWithMemberVisibleTo_owner() {
            // given
            Project project = createHiddenProject("비노출 프로젝트", ProjectStatus.RECRUITING);

            // when
            Optional<Project> result = projectRepository.findByIdWithMemberVisibleTo(
                    project.getId(), testMember.getId());

            // then
            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("비노출 프로젝트는 다른 회원에게는 조회되지 않는다")
        void findByIdWithMemberVisibleTo_otherMember() {
            // given
            Project project = createHiddenProject("비노출 프로젝트", ProjectStatus.RECRUITING);
            Member other = memberRepository.save(Member.builder()
                    .clerkId("clerk_repo_other")
                    .name("다른회원")
                    .nickname("repoother")
                    .mainType(MemberMainType.DEVELOPER)
                    .disclosure(true)
                    .used(MemberStatus.ACTIVE)
                    .build());

            // when
            Optional<Project> result = projectRepository.findByIdWithMemberVisibleTo(
                    project.getId(), other.getId());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("비로그인(viewerId가 null)이면 비노출 프로젝트는 조회되지 않는다")
        void findByIdWithMemberVisibleTo_anonymous() {
            // given
            Project project = createHiddenProject("비노출 프로젝트", ProjectStatus.RECRUITING);

            // when
            Optional<Project> result = projectRepository.findByIdWithMemberVisibleTo(project.getId(), null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("삭제된 프로젝트는 작성자에게도 조회되지 않는다")
        void findByIdWithMemberVisibleTo_deleted() {
            // given
            Project project = createProject("삭제된 프로젝트", ProjectStatus.DELETED);

            // when
            Optional<Project> result = projectRepository.findByIdWithMemberVisibleTo(
                    project.getId(), testMember.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findWeeklyBestProjects")
    class FindWeeklyBestProjectsTest {

        @Test
        @DisplayName("삭제된 프로젝트를 제외하고 조회한다")
        void findWeeklyBestProjects_excludeDeleted() {
            // given
            createProject("정상 프로젝트 1", ProjectStatus.RECRUITING);
            createProject("정상 프로젝트 2", ProjectStatus.IN_PROGRESS);
            createProject("삭제된 프로젝트", ProjectStatus.DELETED);

            // when
            List<Project> result = projectRepository.findWeeklyBestProjects(false);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).noneMatch(p -> p.getStatus() == ProjectStatus.DELETED);
        }

        @Test
        @DisplayName("비노출 처리된 프로젝트를 제외하고 조회한다")
        void findWeeklyBestProjects_excludeHidden() {
            // given
            createProject("정상 프로젝트", ProjectStatus.RECRUITING);
            createHiddenProject("비노출 프로젝트", ProjectStatus.RECRUITING);

            // when
            List<Project> result = projectRepository.findWeeklyBestProjects(false);

            // then
            assertThat(result).hasSize(1);
            assertThat(result).noneMatch(Project::isHidden);
        }
    }

    @Nested
    @DisplayName("incrementViewCount")
    class IncrementViewCountTest {

        @Test
        @DisplayName("조회수가 원자적으로 증가한다")
        void incrementViewCount_success() {
            // given
            Project project = createProject("조회수 프로젝트", ProjectStatus.RECRUITING);
            Long initialTotal = project.getTotalViewCount();
            Long initialWeekly = project.getWeeklyViewCount();

            // when
            projectRepository.incrementViewCount(project.getId());
            entityManager.flush();
            entityManager.clear();

            // then
            Project updated = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(updated.getTotalViewCount()).isEqualTo(initialTotal + 1);
            assertThat(updated.getWeeklyViewCount()).isEqualTo(initialWeekly + 1);
        }
    }

    @Nested
    @DisplayName("findVisibleByIdIn")
    class FindVisibleByIdInTest {

        @Test
        @DisplayName("ID 목록으로 프로젝트를 일괄 조회한다")
        void findVisibleByIdIn_success() {
            // given
            Project project1 = createProject("프로젝트 1", ProjectStatus.RECRUITING);
            Project project2 = createProject("프로젝트 2", ProjectStatus.RECRUITING);
            createProject("프로젝트 3", ProjectStatus.RECRUITING);

            // when
            List<Project> result = projectRepository.findVisibleByIdIn(
                    List.of(project1.getId(), project2.getId()));

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("비노출 처리된 프로젝트는 제외된다")
        void findVisibleByIdIn_excludeHidden() {
            // given
            Project visible = createProject("노출 프로젝트", ProjectStatus.RECRUITING);
            Project hidden = createHiddenProject("비노출 프로젝트", ProjectStatus.RECRUITING);

            // when
            List<Project> result = projectRepository.findVisibleByIdIn(
                    List.of(visible.getId(), hidden.getId()));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("노출 프로젝트");
        }

        @Test
        @DisplayName("삭제된 프로젝트는 제외된다")
        void findVisibleByIdIn_excludeDeleted() {
            // given
            Project visible = createProject("노출 프로젝트", ProjectStatus.RECRUITING);
            Project deleted = createProject("삭제된 프로젝트", ProjectStatus.DELETED);

            // when
            List<Project> result = projectRepository.findVisibleByIdIn(
                    List.of(visible.getId(), deleted.getId()));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("노출 프로젝트");
        }
    }

    @Nested
    @DisplayName("rotateWeeklyViewCount")
    class RotateWeeklyViewCountTest {

        @Test
        @DisplayName("주간 조회수가 이전 주 조회수로 이동하고 리셋된다")
        void rotateWeeklyViewCount_success() {
            // given
            Project project = createProject("조회수 리셋 프로젝트", ProjectStatus.RECRUITING);
            // 조회수 증가
            projectRepository.incrementViewCount(project.getId());
            projectRepository.incrementViewCount(project.getId());
            entityManager.flush();
            entityManager.clear();

            LocalDate resetDate = LocalDate.now();

            // when
            int updated = projectRepository.rotateWeeklyViewCount(resetDate);
            entityManager.flush();
            entityManager.clear();

            // then
            assertThat(updated).isGreaterThanOrEqualTo(1);
            Project result = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(result.getWeeklyViewCount()).isEqualTo(0L);
            assertThat(result.getPreviousWeekViewCount()).isEqualTo(2L);
        }
    }
}
