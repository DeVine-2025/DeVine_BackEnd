package com.umc.devine.admin.payment.enums;

import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.enums.TransactionStatus;

/**
 * 어드민 화면용 파생 결제 상태.
 * Payment에는 상태 컬럼이 없어 PAYMENT 트랜잭션 상태와 최신 환불 상태를 조합해 계산한다.
 */
public enum AdminPaymentStatus {
    PAID,
    FAILED,
    REFUND_IN_PROGRESS,
    REFUNDED,
    REFUND_FAILED,
    REFUND_UNKNOWN;

    /** @param refundStatus 최신 환불 로우의 상태. 환불 이력이 없으면 null. */
    public static AdminPaymentStatus of(TransactionStatus transactionStatus, RefundStatus refundStatus) {
        if (refundStatus == null) {
            return transactionStatus == TransactionStatus.PAID ? PAID : FAILED;
        }
        return switch (refundStatus) {
            case IN_PROGRESS -> REFUND_IN_PROGRESS;
            case COMPLETED -> REFUNDED;
            case FAILED -> REFUND_FAILED;
            case UNKNOWN -> REFUND_UNKNOWN;
        };
    }
}
