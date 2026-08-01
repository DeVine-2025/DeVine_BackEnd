package com.umc.devine.domain.payment.repository.querydsl;

import com.umc.devine.domain.payment.repository.projection.AdminPaymentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface PaymentQueryDsl {

    /**
     * 어드민 결제 목록 조회. 모든 조건은 선택적이며, 기간은 PAYMENT 트랜잭션의 paidAt 기준이다.
     *
     * @param memberNickname 정확 일치. 존재하지 않는 닉네임이면 빈 페이지다.
     * @param paidFrom 포함 (null이면 하한 없음)
     * @param paidUntil 미포함 (null이면 상한 없음)
     */
    Page<AdminPaymentSummary> searchForAdmin(
            String memberNickname,
            Long ticketProductId,
            LocalDateTime paidFrom,
            LocalDateTime paidUntil,
            Pageable pageable
    );
}
