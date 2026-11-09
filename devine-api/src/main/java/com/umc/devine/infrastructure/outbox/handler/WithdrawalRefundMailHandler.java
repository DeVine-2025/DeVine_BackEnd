package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.infrastructure.email.EmailSender;
import com.umc.devine.infrastructure.outbox.OutboxTaskHandler;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.WithdrawalRefundMailPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 발송에 실패하면 예외를 던져 아웃박스가 재시도한다. 닉네임은 넣지 않는다. */
@Component
public class WithdrawalRefundMailHandler implements OutboxTaskHandler {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PaymentRefundRepository paymentRefundRepository;
    private final EmailSender emailSender;
    private final OutboxTaskWriter outboxTaskWriter;
    private final String operatorEmail;

    public WithdrawalRefundMailHandler(
            PaymentRefundRepository paymentRefundRepository,
            EmailSender emailSender,
            OutboxTaskWriter outboxTaskWriter,
            @Value("${withdrawal.operator-email:projectdevine2025@gmail.com}") String operatorEmail
    ) {
        this.paymentRefundRepository = paymentRefundRepository;
        this.emailSender = emailSender;
        this.outboxTaskWriter = outboxTaskWriter;
        this.operatorEmail = operatorEmail;
    }

    @Override
    public OutboxTaskType type() {
        return OutboxTaskType.WITHDRAWAL_REFUND_MAIL;
    }

    @Override
    public void handle(OutboxTask task) {
        WithdrawalRefundMailPayload payload = outboxTaskWriter.readPayload(task, WithdrawalRefundMailPayload.class);
        List<PaymentRefund> refunds = paymentRefundRepository.findAllById(payload.refundIds()).stream()
                .sorted(Comparator.comparing(PaymentRefund::getId))
                .toList();
        if (refunds.isEmpty()) {
            return;
        }

        emailSender.sendOrThrow(operatorEmail, subject(refunds), body(refunds));
    }

    static String subject(List<PaymentRefund> refunds) {
        long total = refunds.stream().mapToLong(r -> r.getRefundAmount() == null ? 0 : r.getRefundAmount()).sum();
        return "[탈퇴 환불 신청] 결제 %d건 / 총 %s원".formatted(refunds.size(), won(total));
    }

    private static String body(List<PaymentRefund> refunds) {
        StringBuilder rows = new StringBuilder();
        for (PaymentRefund refund : refunds) {
            Payment payment = refund.getPayment();
            rows.append("""
                    <tr><td>%d</td><td>%s</td><td>%s</td><td>%d개</td><td>%s원</td><td>%s</td><td>%s</td></tr>
                    """.formatted(
                    payment.getId(),
                    escape(payment.getPortonePaymentId()),
                    format(paidAt(payment)),
                    refund.getRefundCreditCount() == null ? 0 : refund.getRefundCreditCount(),
                    won(refund.getRefundAmount() == null ? 0 : refund.getRefundAmount()),
                    format(refund.getCreatedAt()),
                    refund.getContactEmail() == null ? "(없음)" : escape(refund.getContactEmail())));
        }
        return """
                회원 탈퇴로 남은 생성권 환불 신청이 접수됐습니다. 3영업일 안에 처리해 주세요.<br>
                <b>결제 전체를 취소하지 말고, 아래 환불 금액만 부분 취소해 주세요.</b><br>
                처리 후 관리자 API(PATCH /admin/v1/payments/refunds/{refundId}/complete)로 완료 처리하면 연락 이메일이 지워집니다.<br><br>
                <table border="1" cellpadding="4" cellspacing="0">
                <tr><th>결제 ID</th><th>포트원 결제 ID</th><th>결제일</th><th>환불 수량</th><th>환불 금액</th><th>신청일</th><th>환불 연락 이메일</th></tr>
                %s</table>
                """.formatted(rows);
    }

    private static LocalDateTime paidAt(Payment payment) {
        return payment.getTransactions().stream()
                .filter(t -> t.getType() == TransactionType.PAYMENT && t.getStatus() == TransactionStatus.PAID)
                .map(Transaction::getPaidAt)
                .findFirst()
                .orElse(payment.getCreatedAt());
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME);
    }

    private static String won(long amount) {
        return NumberFormat.getNumberInstance(Locale.KOREA).format(amount);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
