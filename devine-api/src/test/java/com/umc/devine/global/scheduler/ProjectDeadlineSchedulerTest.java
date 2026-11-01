package com.umc.devine.global.scheduler;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.notification.entity.Notification;
import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.repository.NotificationRepository;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.*;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectDeadlineSchedulerTest extends IntegrationTestSupport {

    private static final LocalDate DEADLINE = LocalDate.of(2026, 10, 31);
    private static final LocalDate NEXT_DAY = DEADLINE.plusDays(1);

    @Autowired private ProjectDeadlineScheduler scheduler;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private MatchingRepository matchingRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private CategoryRepository categoryRepository;

    private Member pm;
    private Member dev;

    @BeforeEach
    void setUp() {
        pm = saveMember("clerk_deadline_pm", "dlpm", MemberMainType.PM);
        dev = saveMember("clerk_deadline_dev", "dldev", MemberMainType.DEVELOPER);
    }

    private Member saveMember(String clerkId, String nickname, MemberMainType type) {
        return memberRepository.save(Member.builder()
                .clerkId(clerkId).name(nickname).nickname(nickname)
                .mainType(type).disclosure(true).used(MemberStatus.ACTIVE)
                .build());
    }

    private Project saveProject(ProjectStatus status) {
        return projectRepository.save(Project.builder()
                .name("배치 프로젝트").content("소개").status(status)
                .projectField(ProjectField.WEB).mode(ProjectMode.ONLINE)
                .durationRange(DurationRange.ONE_TO_THREE).location("온라인")
                .recruitmentDeadline(DEADLINE)
                .category(categoryRepository.findByGenre(CategoryGenre.ECOMMERCE).orElseThrow())
                .member(pm)
                .build());
    }

    private void savePending(Project project) {
        matchingRepository.save(Matching.builder()
                .project(project).member(dev).matchingType(MatchingType.APPLY)
                .status(MatchingStatus.PENDING).part(ProjectPart.BACKEND)
                .build());
    }

    private List<Notification> notificationsOf(NotificationType type) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getType() == type && n.getReceiver().getId().equals(pm.getId()))
                .toList();
    }

    @Test
    @DisplayName("P6: 마감일 다음 날 대기 건이 없으면 마감된다")
    void nextDay_noPending_closes() {
        Project project = saveProject(ProjectStatus.RECRUITING);

        scheduler.run(NEXT_DAY);

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getStatus()).isEqualTo(ProjectStatus.CLOSED);
    }

    @Test
    @DisplayName("P7: 마감일 다음 날 대기 건이 있으면 마감일 지남이 되고 PM에게 알린다")
    void nextDay_withPending_deadlinePassed() {
        Project project = saveProject(ProjectStatus.RECRUITING);
        savePending(project);

        scheduler.run(NEXT_DAY);

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getStatus()).isEqualTo(ProjectStatus.DEADLINE_PASSED);
        assertThat(notificationsOf(NotificationType.PROJECT_DEADLINE_PASSED))
                .singleElement()
                .satisfies(n -> assertThat(n.getContent()).contains("1건"));
    }

    @Test
    @DisplayName("P10: 마감일 당일에는 상태를 바꾸지 않고 PM에게 오늘까지라고 알린다")
    void deadlineDay_notifiesOnly() {
        Project project = saveProject(ProjectStatus.RECRUITING);
        savePending(project);

        scheduler.run(DEADLINE);

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getStatus()).isEqualTo(ProjectStatus.RECRUITING);
        assertThat(notificationsOf(NotificationType.PROJECT_DEADLINE_TODAY))
                .singleElement()
                .satisfies(n -> assertThat(n.getContent()).contains("오늘까지", "1건"));
    }

    @Test
    @DisplayName("안전망: 마감일 지남인데 대기 건이 없으면 마감한다")
    void sweep_deadlinePassedWithoutPending_closes() {
        Project project = saveProject(ProjectStatus.DEADLINE_PASSED);

        scheduler.run(NEXT_DAY);

        assertThat(projectRepository.findById(project.getId()).orElseThrow().getStatus()).isEqualTo(ProjectStatus.CLOSED);
    }
}
