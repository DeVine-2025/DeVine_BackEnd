package com.umc.devine.domain.project.scheduler;

import com.umc.devine.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyViewCountResetScheduler {

    private final ProjectRepository projectRepository;

    /**
     * 매주 월요일 00:00:01에 주간 조회수 회전
     * - weeklyViewCount → previousWeekViewCount 이동 (지난 주 조회수로 표시용)
     * - weeklyViewCount = 0 (새 주 집계 시작)
     * - 원자적 UPDATE로 동시성 안전하게 처리
     */
    @Scheduled(cron = "1 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void rotateWeeklyViewCount() {
        log.info("[WeeklyViewCountResetScheduler] 주간 조회수 리셋 스케줄러 시작");

        try {
            LocalDate currentMonday = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            // 원자적 UPDATE: weeklyViewCount → previousWeekViewCount, weeklyViewCount = 0
            int updatedCount = projectRepository.rotateWeeklyViewCount(currentMonday);

            log.info("[WeeklyViewCountResetScheduler] 주간 조회수 리셋 완료 - 기준일: {}, 업데이트된 프로젝트 수: {}",
                    currentMonday, updatedCount);
        } catch (Exception e) {
            log.error("[WeeklyViewCountResetScheduler] 주간 조회수 리셋 실패", e);
        }
    }
}