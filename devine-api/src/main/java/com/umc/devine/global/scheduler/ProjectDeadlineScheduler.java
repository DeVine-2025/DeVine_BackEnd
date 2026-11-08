package com.umc.devine.global.scheduler;

import com.umc.devine.domain.notification.enums.NotificationType;
import com.umc.devine.domain.notification.service.command.NotificationCommandService;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.helper.ProjectLifecycleHelper;
import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * 모집 마감일 처리. 마감일이 10/31이면 10/31까지 모집하고 11/1 00시에 마감한다.
 * 프로젝트마다 별도 트랜잭션으로 처리해 한 건이 실패해도 나머지는 계속한다.
 * 실패한 건은 조건이 그대로라 다음 실행 때 다시 잡힌다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectDeadlineScheduler {

    private final ProjectRepository projectRepository;
    private final ProjectLifecycleHelper lifecycleHelper;
    private final NotificationCommandService notificationCommandService;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        run(ProjectLifecycleHelper.today());
    }

    // 테스트에서 기준 날짜를 지정할 수 있도록 분리
    void run(LocalDate today) {
        int notified = process(
                projectRepository.findAllByStatusAndRecruitmentDeadline(ProjectStatus.RECRUITING, today),
                this::notifyDeadlineToday);

        int expired = process(
                projectRepository.findAllByStatusAndRecruitmentDeadlineBefore(ProjectStatus.RECRUITING, today),
                project -> closeOrMarkDeadlinePassed(project, today));

        // 안전망: 마감 확인 훅이 빠진 경로가 있어도 하루 안에 정리된다
        int swept = process(
                projectRepository.findAllByStatus(ProjectStatus.DEADLINE_PASSED),
                lifecycleHelper::closeIfDeadlinePassedAndNoPending);

        log.info("[ProjectDeadline] 기준일 {} - 당일 알림 {}건, 마감일 경과 처리 {}건, 안전망 확인 {}건",
                today, notified, expired, swept);
    }

    private int process(List<Project> targets, Consumer<Project> action) {
        int success = 0;
        for (Project target : targets) {
            try {
                transactionTemplate.executeWithoutResult(status ->
                        projectRepository.findByIdForUpdate(target.getId()).ifPresent(action));
                success++;
            } catch (Exception e) {
                log.warn("[ProjectDeadline] 처리 실패 - projectId: {}", target.getId(), e);
            }
        }
        return success;
    }

    private void notifyDeadlineToday(Project project) {
        long pending = lifecycleHelper.countPending(project);
        String content = "'%s' 모집은 오늘까지예요. 내일 0시에 마감돼요. 더 모집하려면 마감일을 수정해 주세요."
                .formatted(project.getName());
        if (pending > 0) {
            content += " 대기 중인 지원·제안 %d건이 있어요. 마감 전에 처리해 주세요.".formatted(pending);
        }
        notify(project, NotificationType.PROJECT_DEADLINE_TODAY, content);
    }

    private void closeOrMarkDeadlinePassed(Project project, LocalDate today) {
        // 조회와 처리 사이에 PM이 마감일을 늘렸을 수 있으므로 다시 확인한다
        if (!project.isRecruiting() || !project.getRecruitmentDeadline().isBefore(today)) {
            return;
        }

        long pending = lifecycleHelper.countPending(project);
        if (pending == 0) {
            project.close(null, LocalDateTime.now());
            return;
        }

        project.markDeadlinePassed();
        notify(project, NotificationType.PROJECT_DEADLINE_PASSED,
                "'%s' 마감일이 지났어요. 대기 중인 지원·제안 %d건을 처리하면 마감돼요.".formatted(project.getName(), pending));
    }

    private void notify(Project project, NotificationType type, String content) {
        notificationCommandService.create(type, project.getMember().getId(), null, content, project.getId());
    }
}
