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
        int updated = memberReportCreditRepository.addCreditsByMember(member, 1);
        if (updated == 0) {
            log.error("크레딧 환불 누락 (크레딧 행 없음) — 수동 복구 필요 - memberId: {}", member.getId());
        } else {
            log.info("크레딧 환불 - memberId: {}", member.getId());
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void refundCreditInCurrentTransaction(Member member) {
        int updated = memberReportCreditRepository.addCreditsByMember(member, 1);
        if (updated == 0) {
            log.error("크레딧 환불 실패 (크레딧 행 없음) - memberId: {}", member.getId());
            throw new TicketException(TicketErrorReason.CREDIT_REFUND_FAILED);
        }
        log.info("크레딧 환불 - memberId: {}", member.getId());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initializeCredit(Member member) {
        memberReportCreditRepository.insertIfNotExists(member.getId(), initialCreditCount);
        log.info("초기 크레딧 지급 (멱등) - memberId: {}, count: {}", member.getId(), initialCreditCount);
    }
}
