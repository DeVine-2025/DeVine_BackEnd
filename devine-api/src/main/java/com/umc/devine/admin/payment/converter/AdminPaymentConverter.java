package com.umc.devine.admin.payment.converter;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.admin.payment.enums.AdminPaymentStatus;
import com.umc.devine.domain.payment.entity.CardDetail;
import com.umc.devine.domain.payment.entity.EasyPayDetail;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.repository.projection.AdminPaymentSummary;
import com.umc.devine.domain.ticket.entity.PaymentTicket;

import java.util.List;

public class AdminPaymentConverter {

    public static AdminPaymentResDTO.PaymentSummaryDTO toSummaryDTO(
            AdminPaymentSummary summary, RefundStatus refundStatus
    ) {
        return AdminPaymentResDTO.PaymentSummaryDTO.builder()
                .paymentId(summary.paymentId())
                .memberId(summary.memberId())
                .memberNickname(summary.memberNickname())
                .orderName(summary.orderName())
                .amount(summary.amount())
                .paidAt(summary.paidAt())
                .status(AdminPaymentStatus.of(summary.transactionStatus(), refundStatus))
                .build();
    }

    public static AdminPaymentResDTO.PaymentDetailDTO toDetailDTO(
            Payment payment,
            Transaction paymentTransaction,
            List<PaymentTicket> tickets,
            PaymentRefund refund,
            int remainingReportCredits
    ) {
        RefundStatus refundStatus = refund != null ? refund.getStatus() : null;

        return AdminPaymentResDTO.PaymentDetailDTO.builder()
                .paymentId(payment.getId())
                .portonePaymentId(payment.getPortonePaymentId())
                .memberId(payment.getMember().getId())
                .memberNickname(payment.getMember().getNickname())
                .orderName(payment.getOrderName())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paidAt(paymentTransaction != null ? paymentTransaction.getPaidAt() : null)
                .status(AdminPaymentStatus.of(
                        paymentTransaction != null ? paymentTransaction.getStatus() : null,
                        refundStatus))
                .method(toMethodDTO(paymentTransaction))
                .pgProvider(paymentTransaction != null ? paymentTransaction.getPgProvider() : null)
                .tickets(tickets.stream().map(AdminPaymentConverter::toTicketDTO).toList())
                .remainingReportCredits(remainingReportCredits)
                .refund(refund != null ? toRefundDTO(refund) : null)
                .build();
    }

    private static AdminPaymentResDTO.PaymentMethodDTO toMethodDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        AdminPaymentResDTO.PaymentMethodDTO.PaymentMethodDTOBuilder builder =
                AdminPaymentResDTO.PaymentMethodDTO.builder().method(transaction.getMethod());

        CardDetail card = transaction.getCardDetail();
        EasyPayDetail easyPay = transaction.getEasyPayDetail();

        if (card != null) {
            builder.cardName(card.getCardName())
                    .cardNumber(card.getCardNumber())
                    .cardBrand(card.getCardBrand())
                    .approvalNumber(card.getApprovalNumber())
                    .installmentMonth(card.getInstallmentMonth());
        } else if (easyPay != null) {
            builder.provider(easyPay.getProvider())
                    .cardName(easyPay.getCardName())
                    .cardNumber(easyPay.getCardNumber())
                    .cardBrand(easyPay.getCardBrand())
                    .approvalNumber(easyPay.getApprovalNumber())
                    .installmentMonth(easyPay.getInstallmentMonth());
        }

        return builder.build();
    }

    private static AdminPaymentResDTO.TicketDTO toTicketDTO(PaymentTicket ticket) {
        return AdminPaymentResDTO.TicketDTO.builder()
                .ticketProductId(ticket.getTicketProduct().getId())
                .productName(ticket.getTicketProduct().getName())
                .quantity(ticket.getQuantity())
                .unitPrice(ticket.getUnitPrice())
                .unitCreditAmount(ticket.getUnitCreditAmount())
                .totalCredits(ticket.totalCredits())
                .build();
    }

    private static AdminPaymentResDTO.RefundDTO toRefundDTO(PaymentRefund refund) {
        return AdminPaymentResDTO.RefundDTO.builder()
                .status(refund.getStatus())
                .reason(refund.getReason())
                .cancellationId(refund.getCancellationId())
                .failureReason(refund.getFailureReason())
                .refundedAt(refund.getUpdatedAt())
                .build();
    }
}
