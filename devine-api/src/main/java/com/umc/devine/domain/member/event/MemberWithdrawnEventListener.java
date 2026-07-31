package com.umc.devine.domain.member.event;

import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberWithdrawnEventListener {

    private final ClerkApiClient clerkApiClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberWithdrawn(MemberWithdrawnEvent event) {
        try {
            clerkApiClient.deleteUser(event.getOriginalClerkId());
        } catch (Exception e) {
            log.warn("[MemberWithdrawn] Clerk 사용자 삭제 실패 - memberId: {}", event.getMemberId(), e);
        }
    }
}
