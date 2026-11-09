package com.umc.devine.domain.outbox.enums;

/** 탈퇴 외부 정리는 GITHUB_GRANT_REVOKE, CLERK_USER_DELETE, S3_OBJECT_DELETE 순서로 이어서 실행한다. */
public enum OutboxTaskType {
    GITHUB_GRANT_REVOKE,
    CLERK_USER_DELETE,
    S3_OBJECT_DELETE,
    WITHDRAWAL_REFUND_MAIL
}
