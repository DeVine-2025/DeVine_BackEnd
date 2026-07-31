package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.domain.ticket.repository.CreditRefundRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * credit_refund_request는 금전 청구 기록이라 다른 테이블처럼 무조건 삭제하면 안 된다.
 * 관리자가 처리하지 않은(REQUESTED) 채로 유예기간이 만료된 건은 EXPIRED로 전이해 "미처리로 소멸됐다"는
 * 감사 기록을 남기고, 행 자체는 보존한 채 member_id만 끊는다. 이미 PROCESSED인 건은 상태를 그대로 둔다.
 */
@Component
@Order(50)
@RequiredArgsConstructor
public class CreditRefundRequestHardDeleteHandler implements MemberHardDeleteHandler {

    private final CreditRefundRequestRepository creditRefundRequestRepository;

    @Override
    public void handle(Member member) {
        creditRefundRequestRepository.bulkExpireUnprocessed(member, CreditRefundStatus.REQUESTED, CreditRefundStatus.EXPIRED);
        creditRefundRequestRepository.bulkDetachMember(member);
        creditRefundRequestRepository.bulkNullifyProcessor(member);
    }
}
