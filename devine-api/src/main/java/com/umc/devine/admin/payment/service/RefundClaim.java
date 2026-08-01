package com.umc.devine.admin.payment.service;

/**
 * 환불 선점(claim) 결과. 외부 취소 호출에 필요한 최소 정보만 트랜잭션 밖으로 전달한다.
 *
 * @param refundId          선점된 payment_refund 행 id
 * @param portonePaymentId  PortOne 결제 id
 */
public record RefundClaim(Long refundId, String portonePaymentId) {
}
