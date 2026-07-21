package com.umc.devine.domain.member.event;

import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 탈퇴 이벤트를 수신하여 Clerk 사용자 삭제를 비동기로 처리한다.
 * <p>트랜잭션 커밋 후 실행되므로 DB 트랜잭션 길이/롤백에 영향을 주지 않는다.
 * <p>Clerk 호출 실패는 현재 로그로만 남긴다. 재시도 큐는 후속 작업에서 도입.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberWithdrawnEventListener {

    private final ClerkApiClient clerkApiClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberWithdrawn(MemberWithdrawnEvent event) {
        try {
            clerkApiClient.deleteUser(event.getOriginalClerkId());
            log.info("[MemberWithdrawn] Clerk 사용자 삭제 완료. memberId={}", event.getMemberId());
        } catch (Exception e) {
            log.warn("[MemberWithdrawn] Clerk 사용자 삭제 실패. memberId={}, clerkId={}",
                    event.getMemberId(), event.getOriginalClerkId(), e);
        }
    }
}
