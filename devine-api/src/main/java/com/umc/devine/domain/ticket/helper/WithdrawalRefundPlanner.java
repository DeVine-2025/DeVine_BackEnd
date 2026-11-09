package com.umc.devine.domain.ticket.helper;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 회원의 결제/환불 기록을 읽어 {@link CreditRefundCalculator} 입력을 만든다.
 * 미리보기(FUNC-MEM-001)와 탈퇴 실행(FUNC-MEM-003, 화면 값을 믿지 않고 다시 계산)에서 같이 쓴다.
 */
@Component
@RequiredArgsConstructor
public class WithdrawalRefundPlanner {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final MemberReportCreditRepository memberReportCreditRepository;

    public CreditRefundCalculator.Result plan(Member member, LocalDateTime now) {
        int remaining = memberReportCreditRepository.findByMember(member)
                .map(MemberReportCredit::getRemainingCount)
                .orElse(0);

        List<Payment> payments = paymentRepository.findAllByMemberWithTransactions(member);
        Map<Long, List<PaymentRefund>> refundsByPayment = payments.isEmpty()
                ? Map.of()
                : paymentRefundRepository.findAllByPaymentIdIn(payments.stream().map(Payment::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(refund -> refund.getPayment().getId()));

        List<CreditRefundCalculator.Purchase> purchases = new ArrayList<>();
        for (Payment payment : payments) {
            toPurchase(payment, refundsByPayment.getOrDefault(payment.getId(), List.of()))
                    .ifPresent(purchases::add);
        }

        return CreditRefundCalculator.calculate(remaining, purchases, now);
    }

    private Optional<CreditRefundCalculator.Purchase> toPurchase(Payment payment, List<PaymentRefund> refunds) {
        // 0원 결제(100% 쿠폰)는 유료 결제가 아니므로 무상분으로 본다
        if (payment.getAmount() == null || payment.getAmount() <= 0) {
            return Optional.empty();
        }
        Optional<Transaction> paid = payment.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.PAYMENT && t.getStatus() == TransactionStatus.PAID)
                .findFirst();
        if (paid.isEmpty()) {
            return Optional.empty();
        }

        int granted = payment.getPaymentTickets().stream()
                .mapToInt(ticket -> ticket.getQuantity() * ticket.getUnitCreditAmount())
                .sum();
        if (granted <= 0) {
            return Optional.empty();
        }

        int alreadyRefunded = 0;
        boolean hasHistory = false;
        for (PaymentRefund refund : refunds) {
            if (refund.getStatus() == RefundStatus.FAILED) {
                continue;
            }
            hasHistory = true;
            // 수량 칸이 없는 행은 관리자 결제 전체 환불이므로 지급 개수 전부를 환불한 것으로 본다
            alreadyRefunded += refund.getRefundCreditCount() != null ? refund.getRefundCreditCount() : granted;
        }

        LocalDateTime paidAt = paid.get().getPaidAt() != null ? paid.get().getPaidAt() : payment.getCreatedAt();

        return Optional.of(new CreditRefundCalculator.Purchase(
                payment.getId(),
                payment.getPortonePaymentId(),
                paidAt,
                payment.getAmount(),
                granted,
                Math.min(alreadyRefunded, granted),
                hasHistory
        ));
    }
}
