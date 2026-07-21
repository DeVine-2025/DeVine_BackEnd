package com.umc.devine.admin.payment.enums;

import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Payment에는 상태 컬럼이 없어 트랜잭션 상태 + 최신 환불 상태로 파생한다.
 * 환불 이력이 있으면 환불 상태가 항상 우선한다.
 */
class AdminPaymentStatusTest {

    @Test
    @DisplayName("환불 이력이 없고 결제가 성공했으면 PAID")
    void noRefund_paid() {
        assertThat(AdminPaymentStatus.of(TransactionStatus.PAID, null))
                .isEqualTo(AdminPaymentStatus.PAID);
    }

    @Test
    @DisplayName("환불 이력이 없고 결제가 성공하지 않았으면 FAILED")
    void noRefund_notPaid() {
        assertThat(AdminPaymentStatus.of(TransactionStatus.FAILED, null))
                .isEqualTo(AdminPaymentStatus.FAILED);
        assertThat(AdminPaymentStatus.of(null, null))
                .isEqualTo(AdminPaymentStatus.FAILED);
    }

    @Test
    @DisplayName("환불 상태가 있으면 결제 트랜잭션 상태와 무관하게 환불 상태를 따른다")
    void refundStatus_wins() {
        assertThat(AdminPaymentStatus.of(TransactionStatus.PAID, RefundStatus.IN_PROGRESS))
                .isEqualTo(AdminPaymentStatus.REFUND_IN_PROGRESS);
        assertThat(AdminPaymentStatus.of(TransactionStatus.PAID, RefundStatus.COMPLETED))
                .isEqualTo(AdminPaymentStatus.REFUNDED);
        assertThat(AdminPaymentStatus.of(TransactionStatus.PAID, RefundStatus.FAILED))
                .isEqualTo(AdminPaymentStatus.REFUND_FAILED);
        assertThat(AdminPaymentStatus.of(TransactionStatus.PAID, RefundStatus.UNKNOWN))
                .isEqualTo(AdminPaymentStatus.REFUND_UNKNOWN);
    }
}
