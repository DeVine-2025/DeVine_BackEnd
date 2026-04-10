package com.umc.devine.domain.member.event;

import com.umc.devine.domain.ticket.service.command.ReportCreditCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원가입 이벤트를 수신하여 초기 리포트 크레딧을 지급하는 리스너
 * 트랜잭션 커밋 후 실행되며, 트랜잭션 관리는 initializeCredit(REQUIRES_NEW)이 담당한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MemberSignedUpEventListener {

    private final ReportCreditCommandService reportCreditCommandService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberSignedUp(MemberSignedUpEvent event) {
        try {
            reportCreditCommandService.initializeCredit(event.member());
        } catch (Exception e) {
            log.warn("초기 크레딧 지급 실패 - memberId: {}", event.member().getId(), e);
        }
    }
}
