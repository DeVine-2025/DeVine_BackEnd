package com.umc.devine.infrastructure.outbox.payload;

import java.util.List;

/** 회원 행을 비우기 전에 담아 둔다. 토큰은 담지 않는다. */
public record MemberExternalCleanupPayload(String clerkId, List<String> s3Keys) {
}
