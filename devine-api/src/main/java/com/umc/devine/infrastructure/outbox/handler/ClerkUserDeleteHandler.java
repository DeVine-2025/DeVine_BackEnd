package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.outbox.OutboxTaskHandler;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 탈퇴 외부 정리 2단계: Clerk 사용자 삭제(01-1 FUNC-MEM-005 2번). 사용자의 세션도 함께 끝난다.
 * 끝나면 S3 이미지 삭제를 기록한다.
 */
@Component
@RequiredArgsConstructor
public class ClerkUserDeleteHandler implements OutboxTaskHandler {

    private final ClerkApiClient clerkApiClient;
    private final OutboxTaskWriter outboxTaskWriter;

    @Override
    public OutboxTaskType type() {
        return OutboxTaskType.CLERK_USER_DELETE;
    }

    @Override
    public void handle(OutboxTask task) {
        MemberExternalCleanupPayload payload = outboxTaskWriter.readPayload(task, MemberExternalCleanupPayload.class);
        clerkApiClient.deleteUser(payload.clerkId());
        nextStep(payload);
    }

    // Clerk 삭제가 최종 실패해도 S3 이미지는 지운다. 탈퇴 상태라 API는 이미 막혀 있다
    @Override
    public void onFinalFailure(OutboxTask task) {
        nextStep(outboxTaskWriter.readPayload(task, MemberExternalCleanupPayload.class));
    }

    private void nextStep(MemberExternalCleanupPayload payload) {
        if (payload.s3Keys() != null && !payload.s3Keys().isEmpty()) {
            // 다음 단계에는 clerk_id가 필요 없으므로 담지 않는다
            outboxTaskWriter.enqueue(OutboxTaskType.S3_OBJECT_DELETE,
                    new MemberExternalCleanupPayload(null, payload.s3Keys()));
        }
    }
}
