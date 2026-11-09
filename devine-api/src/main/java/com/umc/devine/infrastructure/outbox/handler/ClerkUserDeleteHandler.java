package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.outbox.OutboxTaskHandler;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClerkUserDeleteHandler implements OutboxTaskHandler {

    private final ClerkApiClient clerkApiClient;
    private final OutboxTaskWriter outboxTaskWriter;
    private final MemberRepository memberRepository;

    @Override
    public OutboxTaskType type() {
        return OutboxTaskType.CLERK_USER_DELETE;
    }

    @Override
    public void handle(OutboxTask task) {
        MemberExternalCleanupPayload payload = outboxTaskWriter.readPayload(task, MemberExternalCleanupPayload.class);
        if (memberRepository.existsByClerkIdInAnyStatus(payload.clerkId())) {
            log.warn("[Outbox] 같은 Clerk 계정으로 다시 가입한 회원이 있어 Clerk 사용자 삭제를 건너뜀 - taskId: {}", task.getId());
        } else {
            clerkApiClient.deleteUser(payload.clerkId());
        }
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
