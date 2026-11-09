package com.umc.devine.infrastructure.outbox.payload;

import java.util.List;

/**
 * 탈퇴 외부 정리(FUNC-MEM-005) 입력. GITHUB_GRANT_REVOKE → CLERK_USER_DELETE → S3_OBJECT_DELETE가 이어서 넘겨받는다.
 * 트랜잭션 안에서 회원 행을 비우기 전에 담아 둔다. 토큰은 담지 않는다.
 *
 * @param clerkId Clerk 사용자 ID
 * @param s3Keys  지울 S3 객체 키(프로필·프로젝트·에디터 이미지)
 */
public record MemberExternalCleanupPayload(String clerkId, List<String> s3Keys) {
}
