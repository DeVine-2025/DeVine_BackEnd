package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminWithdrawalRefundServiceImpl implements AdminWithdrawalRefundService {

    private final PaymentRefundRepository paymentRefundRepository;

    @Override
    public AdminPaymentResDTO.WithdrawalRefundCompleteDTO complete(Long refundId) {
        PaymentRefund refund = paymentRefundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new PaymentException(PaymentErrorReason.REFUND_NOT_FOUND));

        if (!refund.isWithdrawalRequest() || refund.getStatus() != RefundStatus.REQUESTED) {
            throw new PaymentException(PaymentErrorReason.NOT_WITHDRAWAL_REFUND_REQUEST);
        }

        refund.completeWithdrawalRequest();
        log.info("[WithdrawalRefund] 탈퇴 환불 완료 처리 - refundId: {}, paymentId: {}, amount: {}",
                refund.getId(), refund.getPayment().getId(), refund.getRefundAmount());

        return AdminPaymentResDTO.WithdrawalRefundCompleteDTO.builder()
                .refundId(refund.getId())
                .paymentId(refund.getPayment().getId())
                .status(refund.getStatus())
                .refundAmount(refund.getRefundAmount())
                .refundCreditCount(refund.getRefundCreditCount())
                .build();
    }
}
