package com.umc.devine.global.scheduler;

import com.umc.devine.infrastructure.outbox.OutboxTaskProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 커밋 직후 실행을 놓친 작업(서버 재시작 등)과 재시도 시각이 된 작업을 잡는다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxTaskScheduler {

    private final OutboxTaskProcessor outboxTaskProcessor;

    @Scheduled(
            fixedDelayString = "${outbox.poll-interval:60000}",
            initialDelayString = "${outbox.initial-delay:30000}"
    )
    public void processDueTasks() {
        int processed = outboxTaskProcessor.processDue();
        if (processed > 0) {
            log.info("[OutboxScheduler] 아웃박스 작업 처리 - {}건", processed);
        }
    }
}
