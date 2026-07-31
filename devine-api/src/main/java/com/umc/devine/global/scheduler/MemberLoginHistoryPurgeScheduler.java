package com.umc.devine.global.scheduler;

import com.umc.devine.domain.member.repository.MemberLoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 로그인 이력/접속 IP는 통신비밀보호법에 따라 탈퇴 여부와 무관하게 3개월 후 자동 파기한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberLoginHistoryPurgeScheduler {

    private static final int RETENTION_MONTHS = 3;

    private final MemberLoginHistoryRepository memberLoginHistoryRepository;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void purgeExpiredLoginHistory() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(RETENTION_MONTHS);
        int deleted = memberLoginHistoryRepository.bulkDeleteByLoginAtBefore(threshold);
        log.info("[MemberLoginHistoryPurge] 3개월 경과 로그인 이력 파기 완료 - {}건", deleted);
    }
}
