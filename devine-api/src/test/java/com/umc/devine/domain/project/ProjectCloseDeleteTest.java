package com.umc.devine.domain.project;

import com.umc.devine.admin.project.service.command.ProjectVisibilityCommandService;
import com.umc.devine.domain.bookmark.entity.Bookmark;
import com.umc.devine.domain.bookmark.enums.BookmarkType;
import com.umc.devine.domain.bookmark.repository.BookmarkRepository;
import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.exception.code.MatchingErrorReason;
import com.umc.devine.domain.project.exception.code.ProjectErrorReason;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
import com.umc.devine.domain.project.service.command.ProjectCommandService;
import com.umc.devine.domain.project.service.query.MatchingQueryService;
import com.umc.devine.domain.project.service.query.ProjectQueryService;
import com.umc.devine.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 노션 기능명세서 테스트 시나리오(P1~P19) 중 서비스 단에서 확인 가능한 것.
 * 자동 마감 배치(P6·P7·P10)는 ProjectDeadlineSchedulerTest에서 본다.
 */
class ProjectCloseDeleteTest extends IntegrationTestSupport {

    @Autowired private ProjectCommandService projectCommandService;
    @Autowired private ProjectQueryService projectQueryService;
    @Autowired private MatchingCommandService matchingCommandService;
    @Autowired private MatchingQueryService matchingQueryService;
    @Autowired private ProjectVisibilityCommandService projectVisibilityCommandService;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectRequirementMemberRepository requirementMemberRepository;
    @Autowired private MatchingRepository matchingRepository;
    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private EntityManager em;

    private Member pm;
    private Member dev;
    private Category category;

    @BeforeEach
    void setUp() {
        pm = saveMember("clerk_close_pm", "closepm", MemberMainType.PM);
        dev = saveMember("clerk_close_dev", "closedev", MemberMainType.DEVELOPER);
        category = categoryRepository.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow();
    }

    private Member saveMember(String clerkId, String nickname, MemberMainType type) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId).name(nickname).nickname(nickname)
                .mainType(type).disclosure(true).used(MemberStatus.ACTIVE)
                .build());
    }

    private Project saveProject(ProjectStatus status, LocalDate deadline) {
        Project project = projectRepository.save(Project.builder()
                .name("마감 테스트 프로젝트").content("소개").status(status)
                .projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(deadline).category(category).member(pm)
                .build());
        project.addRequirement(requirementMemberRepository.save(ProjectRequirementMember.builder()
                .project(project).part(ProjectPart.BACKEND).requirementNum(2).currentCount(0)
                .build()));
        return project;
    }

    private Matching saveMatching(Project project, MatchingType type, MatchingStatus status) {
        return matchingRepository.save(Matching.builder()
                .project(project).member(dev).matchingType(type).status(status).part(ProjectPart.BACKEND)
                .build());
    }

    private ProjectReqDTO.UpdateProjectReq updateReq(LocalDate deadline, String content) {
        return ProjectReqDTO.UpdateProjectReq.builder()
                .projectField(ProjectField.WEB).category(CategoryGenre.ECOMMERCE).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(deadline).title("마감 테스트 프로젝트").content(content)
                .build();
    }

    private void assertErrorReason(Runnable call, ProjectErrorReason reason) {
        assertThatThrownBy(call::run)
                .isInstanceOf(ProjectException.class)
                .extracting("reason").isEqualTo(reason);
    }

    @Test
    @DisplayName("P1·P2: 대기 지원이 있으면 마감할 수 없고, 모두 처리하면 마감되고 삭제 가능해진다")
    void close_requiresNoPending() {
        Project project = saveProject(ProjectStatus.RECRUITING, LocalDate.now().plusDays(10));
        Matching apply = saveMatching(project, MatchingType.APPLY, MatchingStatus.PENDING);

        assertErrorReason(() -> projectCommandService.closeProject(pm, project.getId()),
                ProjectErrorReason.PENDING_MATCHINGS_EXIST);
        ProjectResDTO.ManageRes before = projectQueryService.getManageInfo(pm, project.getId());
        assertThat(before.pendingCount()).isEqualTo(1);
        assertThat(before.pendingMatchings().get(0).nickname()).isEqualTo("closedev");
        assertThat(before.canClose()).isFalse();

        matchingCommandService.respondToApplication(pm, apply.getId(), MatchingDecision.REJECT);
        projectCommandService.closeProject(pm, project.getId());

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
        assertThat(projectQueryService.getManageInfo(pm, project.getId()).canDelete()).isTrue();
    }

    @Test
    @DisplayName("P3: 모집 중인 프로젝트는 삭제할 수 없다")
    void delete_recruiting_rejected() {
        Project project = saveProject(ProjectStatus.RECRUITING, LocalDate.now().plusDays(10));

        assertErrorReason(() -> projectCommandService.deleteProject(pm, project.getId()),
                ProjectErrorReason.NOT_DELETABLE);
    }

    @Test
    @DisplayName("진행 중이어도 대기 건이 있으면 삭제할 수 없다")
    void delete_inProgressWithPending_rejected() {
        Project project = saveProject(ProjectStatus.IN_PROGRESS, LocalDate.now().plusDays(10));
        saveMatching(project, MatchingType.APPLY, MatchingStatus.PENDING);

        assertErrorReason(() -> projectCommandService.deleteProject(pm, project.getId()),
                ProjectErrorReason.PENDING_MATCHINGS_EXIST);
    }

    @Test
    @DisplayName("P4: 매칭 이력이 있으면 이름만 남기고 지우며, 지원자 이력에는 삭제된 프로젝트로 보인다")
    void delete_withHistory_keepsNameOnly() {
        Project project = saveProject(ProjectStatus.CLOSED, LocalDate.now().minusDays(1));
        saveMatching(project, MatchingType.APPLY, MatchingStatus.CANCELLED);
        Matching accepted = saveMatching(project, MatchingType.PROPOSE, MatchingStatus.COMPLETED);
        bookmarkRepository.save(Bookmark.builder()
                .member(dev).targetType(BookmarkType.PROJECT).targetId(project.getId()).build());

        projectCommandService.deleteProject(pm, project.getId());
        em.flush();
        em.clear();

        Project deleted = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(ProjectStatus.DELETED);
        assertThat(deleted.getName()).isEqualTo("마감 테스트 프로젝트");
        assertThat(deleted.getContent()).isEmpty();
        assertThat(deleted.isHidden()).isTrue();
        assertThat(requirementMemberRepository.findAllByProject(deleted)).isEmpty();
        assertThat(bookmarkRepository.existsByMemberAndTargetTypeAndTargetId(dev, BookmarkType.PROJECT, project.getId())).isFalse();
        assertThat(projectRepository.findVisibleById(project.getId())).isEmpty();

        MatchingResDTO.ProjectsRes history = matchingQueryService.getProjects(dev, MatchingType.PROPOSE, PageRequest.of(0, 10));
        MatchingResDTO.ProjectMatchingInfo item = history.projects().getContent().get(0);
        assertThat(item.matchingId()).isEqualTo(accepted.getId());
        assertThat(item.projectName()).isEqualTo("마감 테스트 프로젝트");
        assertThat(item.projectDeleted()).isTrue();
    }

    @Test
    @DisplayName("P5: 매칭 이력이 없으면 프로젝트 행까지 지운다")
    void delete_withoutHistory_removesRow() {
        Project project = saveProject(ProjectStatus.CLOSED, LocalDate.now().minusDays(1));
        Long id = project.getId();

        projectCommandService.deleteProject(pm, id);
        em.flush();
        em.clear();

        assertThat(projectRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("P8: 마감일 지남 상태에서 마지막 대기 건을 처리하면 바로 마감된다")
    void deadlinePassed_lastPendingHandled_closes() {
        Project project = saveProject(ProjectStatus.DEADLINE_PASSED, LocalDate.now().minusDays(1));
        Matching apply = saveMatching(project, MatchingType.APPLY, MatchingStatus.PENDING);

        matchingCommandService.respondToApplication(pm, apply.getId(), MatchingDecision.REJECT);

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
    }

    @Test
    @DisplayName("P17: 대기 건이 보낸 제안이면 PM이 제안을 취소해 마감할 수 있다")
    void pendingProposal_cancelByPm_thenClose() {
        Project project = saveProject(ProjectStatus.RECRUITING, LocalDate.now().plusDays(10));
        Matching proposal = saveMatching(project, MatchingType.PROPOSE, MatchingStatus.PENDING);

        assertThatThrownBy(() -> matchingCommandService.cancelProposal(dev, proposal.getId()))
                .isInstanceOf(MatchingException.class)
                .extracting("reason").isEqualTo(MatchingErrorReason.NOT_PROPOSAL_OWNER);

        matchingCommandService.cancelProposal(pm, proposal.getId());
        projectCommandService.closeProject(pm, project.getId());

        assertThat(proposal.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
    }

    @Test
    @DisplayName("P9·P11: 마감일을 오늘 이후로 고치면 마감일 지남·마감 상태에서 다시 모집한다")
    void extendDeadline_reopens() {
        Project passed = saveProject(ProjectStatus.DEADLINE_PASSED, LocalDate.now().minusDays(1));
        Project closed = saveProject(ProjectStatus.CLOSED, LocalDate.now().minusDays(1));

        projectCommandService.updateProject(pm, passed.getId(), updateReq(LocalDate.now().plusDays(14), "소개"));
        projectCommandService.updateProject(pm, closed.getId(), updateReq(LocalDate.now(), "소개"));

        assertThat(passed.getStatus()).isEqualTo(ProjectStatus.RECRUITING);
        assertThat(closed.getStatus()).isEqualTo(ProjectStatus.RECRUITING);
    }

    @Test
    @DisplayName("P19: 마감일이 지난 마감 프로젝트의 소개만 고치면 상태가 그대로다")
    void updateContentOnly_keepsClosed() {
        LocalDate pastDeadline = LocalDate.now().minusDays(3);
        Project project = saveProject(ProjectStatus.CLOSED, pastDeadline);

        projectCommandService.updateProject(pm, project.getId(), updateReq(pastDeadline, "고친 소개"));

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
        assertThat(project.getContent()).isEqualTo("고친 소개");
    }

    @Test
    @DisplayName("P12: PM은 언제든 숨기고 다시 보이게 할 수 있다")
    void pmHideAndUnhide() {
        Project project = saveProject(ProjectStatus.IN_PROGRESS, LocalDate.now().plusDays(10));

        projectCommandService.changeVisibility(pm, project.getId(), true);
        assertThat(project.isHidden()).isTrue();
        assertThat(projectQueryService.getManageInfo(pm, project.getId()).hiddenByAdmin()).isFalse();

        projectCommandService.changeVisibility(pm, project.getId(), false);
        assertThat(project.isHidden()).isFalse();
    }

    @Test
    @DisplayName("P13: 관리자가 숨긴 프로젝트는 PM이 다시 보이게 할 수 없고, 숨김 요청으로 처리자를 덮어쓰지도 못한다")
    void adminHidden_pmCannotUnhide() {
        Project project = saveProject(ProjectStatus.RECRUITING, LocalDate.now().plusDays(10));
        projectVisibilityCommandService.hideForModeration(project.getId(), null);

        projectCommandService.changeVisibility(pm, project.getId(), true);
        assertErrorReason(() -> projectCommandService.changeVisibility(pm, project.getId(), false),
                ProjectErrorReason.HIDDEN_BY_ADMIN);

        assertThat(project.isHidden()).isTrue();
        assertThat(project.isHiddenByAdmin()).isTrue();
        assertThat(projectQueryService.getManageInfo(pm, project.getId()).canToggleHidden()).isFalse();
    }

    @Test
    @DisplayName("상태 변경 API로도 대기 건이 남은 모집 단계를 벗어날 수 없다")
    void changeStatus_withPending_rejected() {
        Project project = saveProject(ProjectStatus.RECRUITING, LocalDate.now().plusDays(10));
        saveMatching(project, MatchingType.APPLY, MatchingStatus.PENDING);

        assertErrorReason(() -> projectCommandService.changeProjectStatus(pm, project.getId(), ProjectStatus.IN_PROGRESS),
                ProjectErrorReason.PENDING_MATCHINGS_EXIST);
    }

    @Test
    @DisplayName("상태 변경 API로 마감일이 지난 프로젝트를 다시 모집 중으로 바꿀 수 없다")
    void changeStatus_toRecruitingAfterDeadline_rejected() {
        Project project = saveProject(ProjectStatus.CLOSED, LocalDate.now().minusDays(1));

        assertErrorReason(() -> projectCommandService.changeProjectStatus(pm, project.getId(), ProjectStatus.RECRUITING),
                ProjectErrorReason.INVALID_RECRUITMENT_DEADLINE);
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.CLOSED);
    }

    @Test
    @DisplayName("작성자가 아니면 관리 정보·마감·삭제·숨기기를 할 수 없다")
    void notOwner_forbidden() {
        Project project = saveProject(ProjectStatus.CLOSED, LocalDate.now().minusDays(1));

        assertErrorReason(() -> projectQueryService.getManageInfo(dev, project.getId()), ProjectErrorReason.FORBIDDEN_PROJECT_ACCESS);
        assertErrorReason(() -> projectCommandService.closeProject(dev, project.getId()), ProjectErrorReason.FORBIDDEN_PROJECT_ACCESS);
        assertErrorReason(() -> projectCommandService.deleteProject(dev, project.getId()), ProjectErrorReason.FORBIDDEN_PROJECT_ACCESS);
        assertErrorReason(() -> projectCommandService.changeVisibility(dev, project.getId(), true), ProjectErrorReason.FORBIDDEN_PROJECT_ACCESS);
    }
}
