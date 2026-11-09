package com.umc.devine.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 작업을 기록한 트랜잭션이 커밋되면 바로 실행한다(01-1 FUNC-MEM-003 4번).
 * 롤백되면 실행하지 않는다. 놓친 작업은 {@link OutboxTaskScheduler}가 주기적으로 다시 잡는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxTaskEventListener {

    private final OutboxTaskProcessor outboxTaskProcessor;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnqueued(OutboxTaskEnqueuedEvent event) {
        outboxTaskProcessor.processDue();
    }
}
