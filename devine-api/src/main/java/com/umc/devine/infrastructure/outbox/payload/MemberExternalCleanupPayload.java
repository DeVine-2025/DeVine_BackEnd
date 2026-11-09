package com.umc.devine.infrastructure.outbox.payload;

import java.util.List;

public record MemberExternalCleanupPayload(String clerkId, List<String> s3Keys) {
}
