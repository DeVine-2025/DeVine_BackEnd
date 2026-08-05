package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.converter.AdminPaymentConverter;
import com.umc.devine.admin.payment.dto.AdminPaymentReqDTO;
import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.payment.repository.projection.AdminPaymentSummary;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.PaymentTicketRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentQueryServiceImpl implements AdminPaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final PaymentTicketRepository paymentTicketRepository;
    private final MemberReportCreditRepository memberReportCreditRepository;

    @Override
    public PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO> searchPayments(
            AdminPaymentReqDTO.SearchDTO condition, Pageable pageable
    ) {
        Page<AdminPaymentSummary> page = paymentRepository.searchForAdmin(
                condition.memberNickname(),
                condition.ticketProductId(),
                condition.paidFrom(),
                condition.paidUntil(),
                pageable
        );

        if (page.isEmpty()) {
            return PagedResponse.empty(pageable);
        }

        Map<Long, RefundStatus> refundStatuses = latestRefundStatuses(
                page.getContent().stream().map(AdminPaymentSummary::paymentId).toList());

        List<AdminPaymentResDTO.PaymentSummaryDTO> content = page.getContent().stream()
                .map(summary -> AdminPaymentConverter.toSummaryDTO(
                        summary, refundStatuses.get(summary.paymentId())))
                .toList();

        return PagedResponse.of(page, content);
    }

    @Override
    public AdminPaymentResDTO.PaymentDetailDTO getPaymentDetail(Long paymentId) {
        Payment payment = paymentRepository.findByIdWithMemberAndTransactions(paymentId)
                .orElseThrow(() -> new PaymentException(PaymentErrorReason.PAYMENT_NOT_FOUND));

        Transaction paymentTransaction = payment.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.PAYMENT)
                .findFirst()
                .orElse(null);

        List<PaymentTicket> tickets = paymentTicketRepository.findAllByPaymentIdWithProduct(paymentId);
        PaymentRefund refund = paymentRefundRepository.findTopByPaymentIdOrderByIdDesc(paymentId).orElse(null);
        int remainingCredits = memberReportCreditRepository.findByMember(payment.getMember())
                .map(credit -> credit.getRemainingCount())
                .orElse(0);

        return AdminPaymentConverter.toDetailDTO(
                payment, paymentTransaction, tickets, refund, remainingCredits);
    }

    /** 환불 재시도로 로우가 여러 개일 수 있어 id 오름차순 결과의 마지막 값을 남긴다. */
    private Map<Long, RefundStatus> latestRefundStatuses(List<Long> paymentIds) {
        Map<Long, RefundStatus> statuses = new HashMap<>();
        for (PaymentRefund refund : paymentRefundRepository.findAllByPaymentIdIn(paymentIds)) {
            statuses.put(refund.getPayment().getId(), refund.getStatus());
        }
        return statuses;
    }
}
