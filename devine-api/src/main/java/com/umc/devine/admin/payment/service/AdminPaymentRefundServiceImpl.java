package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.infrastructure.portone.PortOneClient;
import com.umc.devine.infrastructure.portone.dto.CancelOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 환불 오케스트레이터(트랜잭션 없음). 외부 취소 호출을 트랜잭션 밖에서 수행하고,
 * 그 결과({@link CancelOutcome})에 따라 트랜잭션 경계 빈({@link RefundTxService})에 반영을 위임한다.
 *
 * <p>핵심: 타임아웃/결과 불명은 절대 FAILED로 종료하지 않는다(UNKNOWN, 대사 대상). 이것이 이중 환불 방지의 실질이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentRefundServiceImpl implements AdminPaymentRefundService {

    private final RefundTxService tx;
    private final PortOneClient portOneClient;

    @Override
    public AdminPaymentResDTO.RefundResultDTO refund(Long paymentId, String reason) {
        // 1. 검증 + 선점(한 트랜잭션). 활성 환불 부분 유니크 인덱스가 동시 중복을 막는다.
        RefundClaim claim = tx.claim(paymentId, reason);

        // 2. 외부 취소 호출 (트랜잭션 밖)
        CancelOutcome outcome = portOneClient.cancelPayment(claim.portonePaymentId(), reason);

        // 3. 결과 반영
        return switch (outcome) {
            case CancelOutcome.Succeeded s -> settle(claim, s);
            case CancelOutcome.Rejected r -> {
                tx.markFailed(claim.refundId(), r.reason());
                throw new PaymentException(PaymentErrorReason.REFUND_REJECTED);
            }
            case CancelOutcome.Unknown u -> {
                tx.markUnknown(claim.refundId(), null, u.detail());
                log.error("PG 취소 결과 불명 - refundId={}, detail={}", claim.refundId(), u.detail());
                throw new PaymentException(PaymentErrorReason.REFUND_RESULT_UNKNOWN);
            }
        };
    }

    private AdminPaymentResDTO.RefundResultDTO settle(RefundClaim claim, CancelOutcome.Succeeded cancel) {
        if (cancel.alreadyCancelled()) {
            log.info("이미 취소된 결제를 성공으로 흡수 - refundId={}, cancellationId={}",
                    claim.refundId(), cancel.cancellationId());
        }
        try {
            return tx.complete(claim.refundId(), cancel);
        } catch (RuntimeException e) {
            // PG는 취소됨, DB 반영 실패 → 대사 대상(UNKNOWN, 취소 id 보존). 절대 FAILED가 아니다.
            tx.markUnknown(claim.refundId(), cancel.cancellationId(), "DB 반영 실패: " + e.getMessage());
            log.error("환불 DB 반영 실패(PG 취소 완료) - refundId={}, cancellationId={}",
                    claim.refundId(), cancel.cancellationId(), e);
            throw new PaymentException(PaymentErrorReason.REFUND_SETTLEMENT_FAILED);
        }
    }
}
