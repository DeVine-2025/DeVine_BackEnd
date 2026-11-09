package com.umc.devine.domain.outbox.enums;

public enum OutboxTaskType {
    GITHUB_GRANT_REVOKE,
    CLERK_USER_DELETE,
    S3_OBJECT_DELETE,
    WITHDRAWAL_REFUND_MAIL
}
