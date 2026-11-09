package com.umc.devine.domain.outbox.enums;

/**
 * 아웃박스 작업 상태. 성공한 작업은 행을 지우므로 완료 상태는 없다.
 */
public enum OutboxTaskStatus {
    /** 실행 대기 또는 재시도 대기. */
    PENDING,
    /** 재시도 한도를 넘겨 최종 실패. 운영자에게 알리고 행을 남긴다. */
    FAILED
}
