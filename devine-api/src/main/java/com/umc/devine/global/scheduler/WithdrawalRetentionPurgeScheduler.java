package com.umc.devine.global.scheduler;

import com.umc.devine.domain.member.helper.WithdrawalProperties;
import com.umc.devine.domain.member.service.command.WithdrawalRetentionPurgeService;
import com.umc.devine.infrastructure.email.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 보관 만료 파기 배치(01-1 FUNC-MEM-007). 하루 1회 실행한다.
 * 단계마다 따로 실행해 한 단계가 실패해도 나머지는 진행하고, 실패하면 운영자에게 알린다(다음 날 다시 처리된다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "withdrawal.retention-purge.enabled", havingValue = "true", matchIfMissing = true)
public class WithdrawalRetentionPurgeScheduler {

    private final WithdrawalRetentionPurgeService purgeService;
    private final EmailSender emailSender;
    private final WithdrawalProperties withdrawalProperties;

    @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
    public void purge() {
        LocalDateTime now = LocalDateTime.now();
        List<String> failures = new ArrayList<>();

        run("매칭 이력", failures, () -> {
            int[] result = purgeService.purgeMatchingHistory(now);
            return "내용 비움 %d건, 행 삭제 %d건".formatted(result[0], result[1]);
        });
        run("이름만 남은 프로젝트", failures, () -> "삭제 %d건".formatted(purgeService.purgeOrphanDeletedProjects()));
        run("최소식별정보", failures, () -> "삭제 %d건".formatted(purgeService.purgeExpiredRestrictions(now)));
        run("결제/환불 기록(5년)", failures, () -> "삭제 %d건".formatted(purgeService.purgeExpiredPayments(now)));

        if (!failures.isEmpty()) {
            emailSender.send(withdrawalProperties.getOperatorEmail(),
                    "[탈퇴 보관 만료 파기] 실패 " + failures.size() + "건",
                    "보관 만료 파기 배치 일부가 실패했습니다. 다음 실행 때 다시 처리됩니다.<br>" + String.join("<br>", failures));
        }
    }

    private void run(String step, List<String> failures, Supplier<String> task) {
        try {
            log.info("[WithdrawalRetentionPurge] {} - {}", step, task.get());
        } catch (Exception e) {
            log.error("[WithdrawalRetentionPurge] {} 실패", step, e);
            failures.add(step + ": " + e.getClass().getSimpleName() + " " + e.getMessage());
        }
    }
}
