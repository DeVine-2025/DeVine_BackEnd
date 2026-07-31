package com.umc.devine.global.scheduler;

import com.umc.devine.domain.project.repository.MatchingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 탈퇴한 회원의 매칭 지원/제안 이력을 개인정보처리방침에 따라 탈퇴 후 1년 뒤 파기한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingHistoryPurgeScheduler {

    private static final int RETENTION_YEARS_AFTER_WITHDRAWAL = 1;

    private final MatchingRepository matchingRepository;

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    @Transactional
    public void purgeWithdrawnMemberMatchingHistory() {
        LocalDateTime threshold = LocalDateTime.now().minusYears(RETENTION_YEARS_AFTER_WITHDRAWAL);
        int deleted = matchingRepository.bulkDeleteByWithdrawnMemberDeletedAtBefore(threshold);
        log.info("[MatchingHistoryPurge] 탈퇴 후 1년 경과 매칭 이력 파기 완료 - {}건", deleted);
    }
}
