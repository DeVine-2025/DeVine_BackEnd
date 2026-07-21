package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.infrastructure.portone.dto.CancelOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환불 트랜잭션 경계 전담 빈. 오케스트레이터(외부 호출)와 분리해 트랜잭션 안/밖을 명확히 나눈다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundTxService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final MemberReportCreditRepository memberReportCreditRepository;

    /**
     * 검증 + 선점을 한 트랜잭션에서 수행한다. 활성 환불 부분 유니크 인덱스가 동시성을 방어하므로,
     * 제약 위반은 이미 환불(진행) 중으로 변환한다.
     */
    @Transactional
    public RefundClaim claim(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorReason.PAYMENT_NOT_FOUND));
        findPaidTransaction(payment);
        try {
            PaymentRefund refund = paymentRefundRepository.saveAndFlush(PaymentRefund.claim(payment, reason));
            return new RefundClaim(refund.getId(), payment.getPortonePaymentId());
        } catch (DataIntegrityViolationException e) {
            throw new PaymentException(PaymentErrorReason.PAYMENT_ALREADY_REFUNDED);
        }
    }

    /** PG 취소 성공 반영: REFUND 트랜잭션 저장 + 크레딧 회수 + 완료 처리. */
    @Transactional
    public AdminPaymentResDTO.RefundResultDTO complete(Long refundId, CancelOutcome.Succeeded cancel) {
        PaymentRefund refund = findRefund(refundId);
        Payment payment = refund.getPayment();

        Transaction original = findPaidTransaction(payment);
        Transaction refundTransaction = Transaction.builder()
                .portoneTransactionId(cancel.cancellationId())   // UNIQUE — 이중 기록 방지
                .payment(payment)
                .type(TransactionType.REFUND)
                .status(TransactionStatus.REFUNDED)
                .method(original.getMethod())
                .pgProvider(original.getPgProvider())
                .amount(cancel.amount())
                .paidAt(cancel.cancelledAt())
                .build();
        payment.addTransaction(refundTransaction);
        paymentRepository.save(payment);

        int revokedCredits = revokeCredits(payment);
        refund.complete(cancel.cancellationId());

        return AdminPaymentResDTO.RefundResultDTO.builder()
                .cancellationId(cancel.cancellationId())
                .amount(cancel.amount())
                .cancelledAt(cancel.cancelledAt())
                .revokedCredits(revokedCredits)
                .build();
    }

    /** PG가 요청을 인지하고 거절 — 종료(FAILED), 재시도 허용. */
    @Transactional
    public void markFailed(Long refundId, String failureReason) {
        findRefund(refundId).markFailed(failureReason);
    }

    /** 취소 결과 불명 — 비종료(UNKNOWN), 대사 대상. cancellationId는 PG 취소가 확인됐을 때만 채운다. */
    @Transactional
    public void markUnknown(Long refundId, String cancellationId, String detail) {
        findRefund(refundId).markUnknown(cancellationId, detail);
    }

    private PaymentRefund findRefund(Long refundId) {
        return paymentRefundRepository.findById(refundId)
                .orElseThrow(() -> new PaymentException(PaymentErrorReason.PAYMENT_NOT_FOUND));
    }

    private Transaction findPaidTransaction(Payment payment) {
        return payment.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.PAYMENT && t.getStatus() == TransactionStatus.PAID)
                .findFirst()
                .orElseThrow(() -> new PaymentException(PaymentErrorReason.PAYMENT_NOT_PAID));
    }

    private int revokeCredits(Payment payment) {
        int granted = payment.getPaymentTickets().stream()
                .mapToInt(pt -> pt.totalCredits())
                .sum();
        if (granted == 0) {
            return 0;
        }
        return memberReportCreditRepository.findByMemberForUpdate(payment.getMember())
                .map(credit -> credit.revokeUpTo(granted))
                .orElse(0);
    }
}
