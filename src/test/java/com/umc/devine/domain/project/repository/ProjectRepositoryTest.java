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
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectRepositoryTest extends IntegrationTestSupport {

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

    @Nested
    @DisplayName("findByIdAndStatusNot")
    class FindByIdAndStatusNotTest {

        @Test
        @DisplayName("삭제되지 않은 프로젝트를 조회한다")
        void findByIdAndStatusNot_success() {
            // given
            Project project = createProject("정상 프로젝트", ProjectStatus.RECRUITING);

            // when
            Optional<Project> result = projectRepository.findByIdAndStatusNot(
                    project.getId(), ProjectStatus.DELETED);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("정상 프로젝트");
        }

        @Test
        @DisplayName("삭제된 프로젝트는 조회되지 않는다")
        void findByIdAndStatusNot_deleted() {
            // given
            Project project = createProject("삭제된 프로젝트", ProjectStatus.DELETED);

            // when
            Optional<Project> result = projectRepository.findByIdAndStatusNot(
                    project.getId(), ProjectStatus.DELETED);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdWithMemberAndStatusNot")
    class FindByIdWithMemberAndStatusNotTest {

        @Test
        @DisplayName("멤버와 카테고리를 함께 조회한다")
        void findByIdWithMember_success() {
            // given
            Project project = createProject("페치조인 프로젝트", ProjectStatus.RECRUITING);

            // when
            Optional<Project> result = projectRepository.findByIdWithMemberAndStatusNot(
                    project.getId(), ProjectStatus.DELETED);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getMember().getNickname()).isEqualTo("repotest");
            assertThat(result.get().getCategory().getGenre()).isEqualTo(CategoryGenre.ECOMMERCE);
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
            List<Project> result = projectRepository.findWeeklyBestProjects(
                    ProjectStatus.DELETED, false);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).noneMatch(p -> p.getStatus() == ProjectStatus.DELETED);
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
    @DisplayName("findAllByIdIn")
    class FindAllByIdInTest {

        @Test
        @DisplayName("ID 목록으로 프로젝트를 일괄 조회한다")
        void findAllByIdIn_success() {
            // given
            Project project1 = createProject("프로젝트 1", ProjectStatus.RECRUITING);
            Project project2 = createProject("프로젝트 2", ProjectStatus.RECRUITING);
            createProject("프로젝트 3", ProjectStatus.RECRUITING);

            // when
            List<Project> result = projectRepository.findAllByIdIn(
                    List.of(project1.getId(), project2.getId()));

            // then
            assertThat(result).hasSize(2);
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
