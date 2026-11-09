package com.umc.devine.domain.payment.entity;

import com.umc.devine.domain.payment.enums.RefundSource;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_refund")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class PaymentRefund extends BaseEntity {

    public static final String WITHDRAWAL_REASON = "회원 탈퇴";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "cancellation_id", length = 255)
    private String cancellationId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RefundSource source = RefundSource.ADMIN;

    /** 탈퇴 환불 금액. 관리자 결제 전체 환불 행은 null이다. */
    @Column(name = "refund_amount")
    private Long refundAmount;

    @Column(name = "refund_credit_count")
    private Integer refundCreditCount;

    /** 탈퇴 환불 연락 이메일. 환불이 끝나면 지운다. */
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    public static PaymentRefund requestForWithdrawal(Payment payment, long refundAmount, int refundCreditCount, String contactEmail) {
        return PaymentRefund.builder()
                .payment(payment)
                .status(RefundStatus.REQUESTED)
                .source(RefundSource.WITHDRAWAL)
                .reason(WITHDRAWAL_REASON)
                .refundAmount(refundAmount)
                .refundCreditCount(refundCreditCount)
                .contactEmail(contactEmail)
                .build();
    }

    public void completeWithdrawalRequest() {
        this.status = RefundStatus.COMPLETED;
        this.contactEmail = null;
    }

    public boolean isWithdrawalRequest() {
        return this.source == RefundSource.WITHDRAWAL;
    }

    /** 외부 취소 호출 전 선점 레코드. 부분 유니크 인덱스가 활성 환불 중복을 막는다. */
    public static PaymentRefund claim(Payment payment, String reason) {
        return PaymentRefund.builder()
                .payment(payment)
                .status(RefundStatus.IN_PROGRESS)
                .reason(reason)
                .build();
    }

    public void complete(String cancellationId) {
        this.status = RefundStatus.COMPLETED;
        this.cancellationId = cancellationId;
    }

    /** PG가 요청을 인지하고 거절 — 종료 상태, 재시도 허용. */
    public void markFailed(String failureReason) {
        this.status = RefundStatus.FAILED;
        this.failureReason = truncate(failureReason);
    }

    /** 취소 결과 불명 — 비종료 상태, 대사 대상. cancellationId는 PG 취소가 확인됐을 때만 채운다. */
    public void markUnknown(String cancellationId, String detail) {
        this.status = RefundStatus.UNKNOWN;
        this.cancellationId = cancellationId;
        this.failureReason = truncate(detail);
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() > 255 ? value.substring(0, 255) : value;
    }
}
