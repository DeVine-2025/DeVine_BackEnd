package com.umc.devine.admin.payment.service;

import com.umc.devine.admin.payment.dto.AdminPaymentReqDTO;
import com.umc.devine.admin.payment.dto.AdminPaymentResDTO;
import com.umc.devine.admin.payment.enums.AdminPaymentStatus;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.entity.CardDetail;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.PaymentRefund;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.RefundStatus;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.repository.PaymentRefundRepository;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.payment.repository.projection.AdminPaymentSummary;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.PaymentTicketRepository;
import com.umc.devine.global.dto.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminPaymentQueryServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentRefundRepository paymentRefundRepository;
    @Mock
    private PaymentTicketRepository paymentTicketRepository;
    @Mock
    private MemberReportCreditRepository memberReportCreditRepository;

    @InjectMocks
    private AdminPaymentQueryServiceImpl service;

    private static final Long PAYMENT_ID = 1L;
    private static final LocalDateTime PAID_AT = LocalDateTime.of(2026, 7, 10, 12, 0);

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(7L).nickname("홍길동").build();
    }

    @Test
    @DisplayName("목록: 환불 이력이 없으면 PAID로 내려간다")
    void search_withoutRefund_isPaid() {
        givenSearchResult();
        given(paymentRefundRepository.findAllByPaymentIdIn(List.of(PAYMENT_ID))).willReturn(List.of());

        PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO> result = search();

        assertThat(result.getContent()).singleElement()
                .satisfies(dto -> {
                    assertThat(dto.status()).isEqualTo(AdminPaymentStatus.PAID);
                    assertThat(dto.memberNickname()).isEqualTo("홍길동");
                    assertThat(dto.paidAt()).isEqualTo(PAID_AT);
                });
    }

    @Test
    @DisplayName("목록: 환불 로우가 여러 개면 가장 최신(마지막) 상태를 따른다")
    void search_multipleRefunds_usesLatest() {
        givenSearchResult();
        // 리포지토리가 id 오름차순으로 반환하므로 뒤쪽이 최신이다.
        given(paymentRefundRepository.findAllByPaymentIdIn(List.of(PAYMENT_ID))).willReturn(List.of(
                refund(RefundStatus.FAILED),
                refund(RefundStatus.COMPLETED)
        ));

        PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO> result = search();

        assertThat(result.getContent().get(0).status()).isEqualTo(AdminPaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("목록: 결과가 없으면 빈 페이지를 반환하고 환불을 조회하지 않는다")
    void search_empty() {
        given(paymentRepository.searchForAdmin(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(), pageable(), 0));

        PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO> result = search();

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("검색 조건의 종료일은 다음 날 00:00 미만으로 변환된다")
    void searchCondition_endDateIsInclusive() {
        AdminPaymentReqDTO.SearchDTO condition = new AdminPaymentReqDTO.SearchDTO(
                null, null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 21));

        assertThat(condition.paidFrom()).isEqualTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThat(condition.paidUntil()).isEqualTo(LocalDateTime.of(2026, 7, 22, 0, 0));
    }

    @Test
    @DisplayName("상세: 결제수단·결제일시·금액·잔여 크레딧을 모두 담아 반환한다")
    void detail_containsRequiredFields() {
        Payment payment = payment();
        given(paymentRepository.findByIdWithMemberAndTransactions(PAYMENT_ID)).willReturn(Optional.of(payment));
        given(paymentTicketRepository.findAllByPaymentIdWithProduct(PAYMENT_ID)).willReturn(List.of(ticket()));
        given(paymentRefundRepository.findTopByPaymentIdOrderByIdDesc(PAYMENT_ID)).willReturn(Optional.empty());
        given(memberReportCreditRepository.findByMember(member))
                .willReturn(Optional.of(MemberReportCredit.of(member, 4)));

        AdminPaymentResDTO.PaymentDetailDTO result = service.getPaymentDetail(PAYMENT_ID);

        assertThat(result.amount()).isEqualTo(12000L);
        assertThat(result.paidAt()).isEqualTo(PAID_AT);
        assertThat(result.status()).isEqualTo(AdminPaymentStatus.PAID);
        assertThat(result.method().method()).isEqualTo(PaymentMethod.CARD);
        assertThat(result.method().cardName()).isEqualTo("신한");
        assertThat(result.pgProvider()).isEqualTo("TOSSPAYMENTS");
        assertThat(result.remainingReportCredits()).isEqualTo(4);
        assertThat(result.refund()).isNull();
        assertThat(result.tickets()).singleElement()
                .satisfies(t -> assertThat(t.totalCredits()).isEqualTo(3));
    }

    @Test
    @DisplayName("상세: 크레딧 로우가 없으면 잔여 수량은 0")
    void detail_noCreditRow_returnsZero() {
        given(paymentRepository.findByIdWithMemberAndTransactions(PAYMENT_ID)).willReturn(Optional.of(payment()));
        given(paymentTicketRepository.findAllByPaymentIdWithProduct(PAYMENT_ID)).willReturn(List.of());
        given(paymentRefundRepository.findTopByPaymentIdOrderByIdDesc(PAYMENT_ID)).willReturn(Optional.empty());
        given(memberReportCreditRepository.findByMember(member)).willReturn(Optional.empty());

        assertThat(service.getPaymentDetail(PAYMENT_ID).remainingReportCredits()).isZero();
    }

    @Test
    @DisplayName("상세: 없는 결제 ID면 PaymentException")
    void detail_notFound() {
        given(paymentRepository.findByIdWithMemberAndTransactions(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPaymentDetail(999L))
                .isInstanceOf(PaymentException.class);
    }

    private PagedResponse<AdminPaymentResDTO.PaymentSummaryDTO> search() {
        return service.searchPayments(
                new AdminPaymentReqDTO.SearchDTO(null, null, null, null), pageable());
    }

    private void givenSearchResult() {
        AdminPaymentSummary summary = new AdminPaymentSummary(
                PAYMENT_ID, 7L, "홍길동", "리포트 생성권 3개 묶음", 12000L, PAID_AT, TransactionStatus.PAID);
        given(paymentRepository.searchForAdmin(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(summary), pageable(), 1));
    }

    private Pageable pageable() {
        return PageRequest.of(0, 10);
    }

    private PaymentRefund refund(RefundStatus status) {
        Payment payment = Payment.builder().id(PAYMENT_ID).build();
        return PaymentRefund.builder().payment(payment).status(status).reason("고객 요청").build();
    }

    private Payment payment() {
        Payment payment = Payment.builder()
                .id(PAYMENT_ID)
                .portonePaymentId("payment_123")
                .member(member)
                .orderName("리포트 생성권 3개 묶음")
                .amount(12000L)
                .currency("KRW")
                .build();

        Transaction transaction = Transaction.builder()
                .portoneTransactionId("tx_123")
                .payment(payment)
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD)
                .pgProvider("TOSSPAYMENTS")
                .amount(12000L)
                .paidAt(PAID_AT)
                .build();
        transaction.addCardDetail(CardDetail.builder().cardName("신한").cardNumber("1234-****").build());
        payment.addTransaction(transaction);

        return payment;
    }

    private PaymentTicket ticket() {
        return PaymentTicket.builder()
                .ticketProduct(TicketProduct.builder().id(2L).name("리포트 생성권 3개 묶음").build())
                .quantity(1)
                .unitPrice(12000L)
                .unitCreditAmount(3)
                .build();
    }
}
