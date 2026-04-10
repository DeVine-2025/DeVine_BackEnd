package com.umc.devine.domain.ticket.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.exception.TicketException;
import com.umc.devine.domain.ticket.exception.code.TicketErrorReason;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportCreditCommandServiceImpl implements ReportCreditCommandService {

    private final MemberReportCreditRepository memberReportCreditRepository;

    @Value("${devine.credit.initial-count:1}")
    private int initialCreditCount;

    @Deprecated // TODO : 비동기 전환 후 제거 예정
    @Override
    public void useCredit(Member member) {
        MemberReportCredit credit = memberReportCreditRepository.findByMemberForUpdate(member)
                .orElseThrow(() -> new TicketException(TicketErrorReason.INSUFFICIENT_CREDITS));
        credit.useCredit(); // 잔여 0이면 내부에서 INSUFFICIENT_CREDITS 예외
        log.info("크레딧 차감 (비관적 락) - memberId: {}, remaining: {}", member.getId(), credit.getRemainingCount());
    }

    @Override
    public void useCreditAtomic(Member member) {
        int updated = memberReportCreditRepository.useCreditByMember(member);
        if (updated == 0) {
            throw new TicketException(TicketErrorReason.INSUFFICIENT_CREDITS);
        }
        log.info("크레딧 차감 (원자적) - memberId: {}", member.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundCredit(Member member) {
        int updated = memberReportCreditRepository.addCreditsWithCap(member, 1, initialCreditCount);
        if (updated == 0) {
            log.error("크레딧 환불 누락 (행 없음 또는 상한 초과) — 수동 복구 필요 - memberId: {}", member.getId());
        } else {
            log.info("크레딧 환불 - memberId: {}", member.getId());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initializeCredit(Member member) {
        // ON CONFLICT DO NOTHING으로 중복/동시 삽입을 트랜잭션 오염 없이 멱등 처리
        memberReportCreditRepository.insertIfNotExists(member.getId(), initialCreditCount);
        log.info("초기 크레딧 지급 (멱등) - memberId: {}, count: {}", member.getId(), initialCreditCount);
    }
}
