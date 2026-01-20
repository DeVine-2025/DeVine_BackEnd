package com.umc.devine.domain.project.scheduler;

import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyViewCountResetScheduler {

    private final ProjectRepository projectRepository;

    // 매주 월요일 00:00:01에 전 주 조회수 리셋
    @Scheduled(cron = "1 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void resetWeeklyViewCount() {
        LocalDate currentMonday = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 리셋이 필요한 프로젝트 조회
        List<Project> projects = projectRepository.findProjectsNeedingWeeklyReset(currentMonday);

        // 주간 조회수 리셋
        int resetCount = 0;
        for (Project project : projects) {
            project.resetWeeklyViewCount(currentMonday);
            resetCount++;
        }
    }
}