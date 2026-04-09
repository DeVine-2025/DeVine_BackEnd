package com.umc.devine.domain.ticket.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.exception.TicketException;
import com.umc.devine.domain.ticket.exception.code.TicketErrorReason;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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
    public void useCredit(Member member) {
        MemberReportCredit credit = memberReportCreditRepository.findByMemberForUpdate(member)
                .orElseThrow(() -> new TicketException(TicketErrorReason.INSUFFICIENT_CREDITS));
        credit.useCredit(); // 잔여 0이면 내부에서 INSUFFICIENT_CREDITS 예외
        log.info("크레딧 차감 - memberId: {}, remaining: {}", member.getId(), credit.getRemainingCount());
    }

    @Override
    public void refundCredit(Member member) {
        int updated = memberReportCreditRepository.addCreditsByMember(member, 1);
        if (updated == 0) {
            log.warn("크레딧 환불 실패 (행 없음) - memberId: {}", member.getId());
        } else {
            log.info("크레딧 환불 - memberId: {}", member.getId());
        }
    }

    @Override
    public void initializeCredit(Member member) {
        try {
            memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(member, initialCreditCount));
            log.info("초기 크레딧 지급 - memberId: {}, count: {}", member.getId(), initialCreditCount);
        } catch (DataIntegrityViolationException ignored) {
            // 동시 요청으로 이미 생성됨 — 무시
        }
    }
}
