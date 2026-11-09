package com.umc.devine.domain.outbox.enums;

/**
 * 커밋 뒤에 실행할 외부 작업 종류.
 * 탈퇴 외부 정리는 GITHUB_GRANT_REVOKE → CLERK_USER_DELETE → S3_OBJECT_DELETE 순서로 이어서 실행한다(FUNC-MEM-005).
 */
public enum OutboxTaskType {
    /** GitHub OAuth 앱 권한 회수. Clerk에서 토큰을 받아야 하므로 Clerk 삭제보다 먼저 한다. */
    GITHUB_GRANT_REVOKE,
    /** Clerk 사용자 삭제. */
    CLERK_USER_DELETE,
    /** 프로필·프로젝트 이미지 S3 객체 삭제. */
    S3_OBJECT_DELETE,
    /** 운영자 탈퇴 환불 신청 알림 메일(FUNC-MEM-004 2번). */
    WITHDRAWAL_REFUND_MAIL
}
