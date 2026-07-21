package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.infrastructure.portone.dto.CancelOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

/**
 * 트랜잭션 경계 빈의 반영 로직(REFUND 트랜잭션 생성 + 크레딧 회수)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class RefundTxServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentRefundRepository paymentRefundRepository;
    @Mock
    private MemberReportCreditRepository memberReportCreditRepository;

    @InjectMocks
    private RefundTxService refundTxService;

    private static final Long REFUND_ID = 10L;
    private static final String CANCELLATION_ID = "cancellation_123";

    private Member member;
    private Payment payment;
    private PaymentRefund refund;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .clerkId("clerk_refund_tx")
                .name("환불세틀")
                .nickname("refundtx")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build();

        payment = Payment.builder()
                .portonePaymentId("payment_123")
                .member(member)
                .orderName("리포트 생성권 5개")
                .amount(4900L)
                .currency("KRW")
                .build();
        payment.addTransaction(Transaction.builder()
                .portoneTransactionId("tx_original")
                .payment(payment)
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD)
                .pgProvider("TOSSPAYMENTS")
                .amount(4900L)
                .paidAt(LocalDateTime.now())
                .build());
        TicketProduct product = TicketProduct.builder()
                .name("리포트 생성권").price(4900L).creditAmount(5).active(true).build();
        payment.addPaymentTicket(PaymentTicket.builder()
                .payment(payment).ticketProduct(product)
                .quantity(1).unitPrice(4900L).unitCreditAmount(5).build());

        refund = PaymentRefund.claim(payment, "고객 요청");
    }

    @Test
    @DisplayName("complete — REFUND 트랜잭션 생성, 크레딧 회수, COMPLETED 전이")
    void complete_success() {
        given(paymentRefundRepository.findById(REFUND_ID)).willReturn(Optional.of(refund));
        MemberReportCredit credit = MemberReportCredit.of(member, 10);
        given(memberReportCreditRepository.findByMemberForUpdate(member)).willReturn(Optional.of(credit));

        AdminPaymentResDTO.RefundResultDTO result = refundTxService.complete(
                REFUND_ID, new CancelOutcome.Succeeded(CANCELLATION_ID, 4900L, LocalDateTime.now(), false));

        assertThat(result.cancellationId()).isEqualTo(CANCELLATION_ID);
        assertThat(result.revokedCredits()).isEqualTo(5);
        assertThat(credit.getRemainingCount()).isEqualTo(5); // 10 - 5
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(refund.getCancellationId()).isEqualTo(CANCELLATION_ID);
        assertThat(payment.getTransactions()).hasSize(2);
        Transaction refundTx = payment.getTransactions().get(1);
        assertThat(refundTx.getType()).isEqualTo(TransactionType.REFUND);
        assertThat(refundTx.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(refundTx.getMethod()).isEqualTo(PaymentMethod.CARD);      // 원본 복사
        assertThat(refundTx.getPgProvider()).isEqualTo("TOSSPAYMENTS");
    }

    @Test
    @DisplayName("complete — 잔액이 지급분보다 적으면 회수 가능한 만큼만(0 클램프)")
    void complete_creditClamp() {
        given(paymentRefundRepository.findById(REFUND_ID)).willReturn(Optional.of(refund));
        MemberReportCredit credit = MemberReportCredit.of(member, 2);
        given(memberReportCreditRepository.findByMemberForUpdate(member)).willReturn(Optional.of(credit));

        AdminPaymentResDTO.RefundResultDTO result = refundTxService.complete(
                REFUND_ID, new CancelOutcome.Succeeded(CANCELLATION_ID, 4900L, LocalDateTime.now(), false));

        assertThat(result.revokedCredits()).isEqualTo(2);
        assertThat(credit.getRemainingCount()).isZero();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
    }

    @Test
    @DisplayName("claim — 결제 완료 트랜잭션이 없으면 PG 호출 전에 거절")
    void claim_rejectsNotPaid() {
        Payment unpaid = Payment.builder()
                .portonePaymentId("payment_unpaid")
                .member(member)
                .orderName("리포트 생성권 5개")
                .amount(4900L)
                .currency("KRW")
                .build();
        given(paymentRepository.findById(1L)).willReturn(Optional.of(unpaid));

        assertThatThrownBy(() -> refundTxService.claim(1L, "고객 요청"))
                .isInstanceOf(PaymentException.class);
        then(paymentRefundRepository).should(never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("markUnknown — 취소 id 보존한 채 UNKNOWN 전이")
    void markUnknown_preservesCancellationId() {
        given(paymentRefundRepository.findById(REFUND_ID)).willReturn(Optional.of(refund));

        refundTxService.markUnknown(REFUND_ID, CANCELLATION_ID, "DB 반영 실패");

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.UNKNOWN);
        assertThat(refund.getCancellationId()).isEqualTo(CANCELLATION_ID);
    }

    @Test
    @DisplayName("markFailed — FAILED 전이(재시도 허용)")
    void markFailed() {
        given(paymentRefundRepository.findById(REFUND_ID)).willReturn(Optional.of(refund));

        refundTxService.markFailed(REFUND_ID, "취소 불가 상태");

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
        assertThat(refund.getFailureReason()).isEqualTo("취소 불가 상태");
    }
}
