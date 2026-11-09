package com.umc.devine.infrastructure.outbox;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskStatus;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.outbox.repository.OutboxTaskRepository;
import com.umc.devine.infrastructure.email.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OutboxTaskProcessor {

    private static final long MAX_BACKOFF_MINUTES = 360;

    private final OutboxTaskRepository outboxTaskRepository;
    private final Map<OutboxTaskType, OutboxTaskHandler> handlers = new EnumMap<>(OutboxTaskType.class);
    private final TransactionTemplate txTemplate;
    private final EmailSender emailSender;
    private final String operatorEmail;
    private final int maxAttempts;
    private final int batchSize;

    public OutboxTaskProcessor(
            OutboxTaskRepository outboxTaskRepository,
            List<OutboxTaskHandler> handlerList,
            PlatformTransactionManager transactionManager,
            EmailSender emailSender,
            @Value("${withdrawal.operator-email:projectdevine2025@gmail.com}") String operatorEmail,
            @Value("${outbox.max-attempts:10}") int maxAttempts,
            @Value("${outbox.batch-size:50}") int batchSize
    ) {
        this.outboxTaskRepository = outboxTaskRepository;
        handlerList.forEach(handler -> handlers.put(handler.type(), handler));
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.emailSender = emailSender;
        this.operatorEmail = operatorEmail;
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
    }

    public int processDue() {
        List<Long> ids = outboxTaskRepository.findDueIds(
                OutboxTaskStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, batchSize));
        ids.forEach(this::processOne);
        return ids.size();
    }

    private void processOne(Long taskId) {
        try {
            txTemplate.executeWithoutResult(status -> execute(taskId));
        } catch (Exception e) {
            log.error("[Outbox] 작업 트랜잭션 롤백 - taskId: {}", taskId, e);
            try {
                txTemplate.executeWithoutResult(status -> outboxTaskRepository.lockDue(taskId, LocalDateTime.now())
                        .ifPresent(task -> recordFailure(task, e, LocalDateTime.now())));
            } catch (Exception recordError) {
                log.error("[Outbox] 실패 기록도 실패 - taskId: {}", taskId, recordError);
            }
        }
    }

    private void execute(Long taskId) {
        LocalDateTime now = LocalDateTime.now();
        OutboxTask task = outboxTaskRepository.lockDue(taskId, now).orElse(null);
        if (task == null) {
            return;
        }

        OutboxTaskHandler handler = handlers.get(task.getTaskType());
        if (handler == null) {
            task.markFailed("실행기가 없는 작업 종류: " + task.getTaskType());
            alertOperator(task);
            return;
        }

        try {
            handler.handle(task);
            outboxTaskRepository.delete(task);
            log.info("[Outbox] 작업 완료 - taskId: {}, type: {}", task.getId(), task.getTaskType());
        } catch (Exception e) {
            recordFailure(task, e, now);
        }
    }

    private void recordFailure(OutboxTask task, Exception e, LocalDateTime now) {
        String error = e.getClass().getSimpleName() + ": " + e.getMessage();
        if (task.getAttemptCount() + 1 >= maxAttempts) {
            task.markFailed(error);
            log.error("[Outbox] 작업 최종 실패 - taskId: {}, type: {}, attempts: {}",
                    task.getId(), task.getTaskType(), task.getAttemptCount(), e);
            OutboxTaskHandler handler = handlers.get(task.getTaskType());
            if (handler != null) {
                handler.onFinalFailure(task);
            }
            alertOperator(task);
        } else {
            LocalDateTime next = now.plusMinutes(backoffMinutes(task.getAttemptCount() + 1));
            task.recordFailure(error, next);
            log.warn("[Outbox] 작업 실패, 재시도 예정 - taskId: {}, type: {}, attempts: {}, next: {}, error: {}",
                    task.getId(), task.getTaskType(), task.getAttemptCount(), next, error);
        }
    }

    static long backoffMinutes(int attempt) {
        long minutes = 1L << Math.min(attempt - 1, 20);
        return Math.min(minutes, MAX_BACKOFF_MINUTES);
    }

    private void alertOperator(OutboxTask task) {
        String subject = "[아웃박스 최종 실패] " + task.getTaskType() + " / 작업 " + task.getId();
        String body = """
                커밋 뒤 외부 작업이 재시도 한도를 넘겨 실패했습니다. 탈퇴 처리 자체는 완료된 상태입니다.<br>
                작업 ID: %d<br>
                작업 종류: %s<br>
                시도 횟수: %d<br>
                마지막 오류: %s<br>
                outbox_task 테이블에서 행을 확인하고 수동으로 처리해 주세요.
                """.formatted(task.getId(), task.getTaskType(), task.getAttemptCount(), task.getLastError());
        emailSender.send(operatorEmail, subject, body);
    }
}
