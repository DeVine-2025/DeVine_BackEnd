package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.outbox.OutboxTaskHandler;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    @Override
    public void onFinalFailure(OutboxTask task) {
        nextStep(outboxTaskWriter.readPayload(task, MemberExternalCleanupPayload.class));
    }

    private void nextStep(MemberExternalCleanupPayload payload) {
        if (payload.s3Keys() != null && !payload.s3Keys().isEmpty()) {
            outboxTaskWriter.enqueue(OutboxTaskType.S3_OBJECT_DELETE,
                    new MemberExternalCleanupPayload(null, payload.s3Keys()));
        }
    }
}
