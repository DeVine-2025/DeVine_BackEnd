package com.umc.devine.domain.payment.repository.projection;

import com.umc.devine.domain.payment.enums.TransactionStatus;

import java.time.LocalDateTime;

/** 어드민 결제 목록 조회용 프로젝션. 파생 상태 계산은 api 모듈 컨버터가 담당한다. */
public record AdminPaymentSummary(
        Long paymentId,
        Long memberId,
        String memberNickname,
        String orderName,
        Long amount,
        LocalDateTime paidAt,
        TransactionStatus transactionStatus
) {}
