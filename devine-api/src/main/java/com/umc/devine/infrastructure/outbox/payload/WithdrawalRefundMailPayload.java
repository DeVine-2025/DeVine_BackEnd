package com.umc.devine.infrastructure.outbox.payload;

import java.util.List;

/**
 * 운영자 탈퇴 환불 신청 알림 메일(FUNC-MEM-004 2번) 입력. 메일 본문은 보낼 때 payment_refund에서 읽는다.
 *
 * @param refundIds 이번 탈퇴로 만든 payment_refund ID
 */
public record WithdrawalRefundMailPayload(List<Long> refundIds) {
}
