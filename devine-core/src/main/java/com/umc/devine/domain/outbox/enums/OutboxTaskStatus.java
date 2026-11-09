package com.umc.devine.domain.outbox.enums;

/** 성공한 작업은 행을 지우므로 완료 상태는 없다. */
public enum OutboxTaskStatus {
    PENDING,
    FAILED
}
