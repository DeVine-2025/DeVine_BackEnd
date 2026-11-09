package com.umc.devine.domain.outbox.entity;

import com.umc.devine.domain.outbox.enums.OutboxTaskStatus;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 트랜잭션 커밋 뒤에 실행할 외부 작업.
 * 같은 트랜잭션에서 저장하므로 롤백되면 작업도 사라진다. 스케줄러는 커밋된 행만 본다.
 * BaseEntity를 상속하지 않는다(created_by에 탈퇴 회원의 clerk_id가 남지 않도록).
 */
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "outbox_task")
public class OutboxTask {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_task_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 40)
    private OutboxTaskType taskType;

    // 작업 입력(JSON). 토큰 같은 비밀값은 넣지 않는다.
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxTaskStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static OutboxTask pending(OutboxTaskType taskType, String payload, LocalDateTime now) {
        return OutboxTask.builder()
                .taskType(taskType)
                .payload(payload)
                .status(OutboxTaskStatus.PENDING)
                .attemptCount(0)
                .nextAttemptAt(now)
                .build();
    }

    /** 실패를 기록하고 다음 시도 시각을 정한다. */
    public void recordFailure(String error, LocalDateTime nextAttemptAt) {
        this.attemptCount++;
        this.lastError = truncate(error);
        this.nextAttemptAt = nextAttemptAt;
    }

    /** 재시도 한도를 넘겨 최종 실패로 남긴다. */
    public void markFailed(String error) {
        this.attemptCount++;
        this.lastError = truncate(error);
        this.status = OutboxTaskStatus.FAILED;
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() > MAX_ERROR_LENGTH ? value.substring(0, MAX_ERROR_LENGTH) : value;
    }
}
