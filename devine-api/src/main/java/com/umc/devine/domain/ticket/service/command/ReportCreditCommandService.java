package com.umc.devine.domain.ticket.service.command;

import com.umc.devine.domain.member.entity.Member;

public interface ReportCreditCommandService {
    /** 크레딧 1 원자적 차감. 잔여 0이면 TicketException(INSUFFICIENT_CREDITS) */
    void useCreditAtomic(Member member);
    /** 크레딧 1 환불 */
    void refundCredit(Member member);
    /** 현재 트랜잭션 안에서 크레딧 1 환불. 환불 행이 없으면 로그만 남긴다. */
    void refundCreditInCurrentTransaction(Member member);
    /** 회원가입 시 초기 크레딧 지급. MemberReportCredit 행이 없으면 생성 */
    void initializeCredit(Member member);
}
