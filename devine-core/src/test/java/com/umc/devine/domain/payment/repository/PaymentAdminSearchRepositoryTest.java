package com.umc.devine.domain.payment.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.domain.payment.entity.Transaction;
import com.umc.devine.domain.payment.enums.PaymentMethod;
import com.umc.devine.domain.payment.enums.TransactionStatus;
import com.umc.devine.domain.payment.enums.TransactionType;
import com.umc.devine.domain.payment.repository.projection.AdminPaymentSummary;
import com.umc.devine.domain.ticket.entity.PaymentTicket;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 어드민 결제 목록 조회 쿼리 검증.
 * 기간 필터가 transaction.paidAt 기준인지, 상품 필터가 행 중복 없이 동작하는지가 핵심이다.
 */
class PaymentAdminSearchRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TicketProductRepository ticketProductRepository;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private Member memberA;
    private Member memberB;
    private TicketProduct single;
    private TicketProduct bundle;

    @BeforeEach
    void setUp() {
        memberA = saveMember("userA");
        memberB = saveMember("userB");
        single = saveProduct("리포트 생성권 1개", 4900L, 1);
        bundle = saveProduct("리포트 생성권 3개 묶음", 12000L, 3);
    }

    @Test
    @DisplayName("조건이 없으면 전체를 paidAt 내림차순으로 반환한다")
    void search_noCondition_sortedByPaidAtDesc() {
        savePayment(memberA, single, LocalDateTime.of(2026, 7, 1, 10, 0));
        savePayment(memberB, bundle, LocalDateTime.of(2026, 7, 20, 10, 0));

        Page<AdminPaymentSummary> result = search(null, null, null, null);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(AdminPaymentSummary::memberNickname)
                .containsExactly("userB", "userA");
    }

    @Test
    @DisplayName("유저 닉네임으로 필터한다")
    void search_byNickname() {
        savePayment(memberA, single, LocalDateTime.of(2026, 7, 1, 10, 0));
        savePayment(memberB, bundle, LocalDateTime.of(2026, 7, 20, 10, 0));

        Page<AdminPaymentSummary> result = search("userA", null, null, null);

        assertThat(result.getContent()).singleElement()
                .satisfies(s -> assertThat(s.memberNickname()).isEqualTo("userA"));
    }

    @Test
    @DisplayName("닉네임은 부분 일치가 아닌 정확 일치로 걸린다")
    void search_byNickname_exactMatch() {
        savePayment(memberA, single, LocalDateTime.of(2026, 7, 1, 10, 0));

        assertThat(search("user", null, null, null).getContent()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 닉네임이면 빈 페이지")
    void search_unknownNickname() {
        savePayment(memberA, single, LocalDateTime.of(2026, 7, 1, 10, 0));

        assertThat(search("없는유저", null, null, null).getContent()).isEmpty();
    }

    @Test
    @DisplayName("상품 ID로 필터하며, 한 결제에 여러 상품이 있어도 행이 중복되지 않는다")
    void search_byProduct_noDuplicateRows() {
        savePayment(memberA, List.of(single, bundle), LocalDateTime.of(2026, 7, 5, 10, 0));
        savePayment(memberB, List.of(single), LocalDateTime.of(2026, 7, 6, 10, 0));

        Page<AdminPaymentSummary> result = search(null, bundle.getId(), null, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).singleElement()
                .satisfies(s -> assertThat(s.memberId()).isEqualTo(memberA.getId()));
    }

    @Test
    @DisplayName("기간 필터는 transaction.paidAt 기준이며 종료 경계 직전까지 포함한다")
    void search_byPaidAtRange() {
        savePayment(memberA, single, LocalDateTime.of(2026, 6, 30, 23, 59));
        savePayment(memberA, single, LocalDateTime.of(2026, 7, 1, 0, 0));
        savePayment(memberA, single, LocalDateTime.of(2026, 7, 21, 23, 59));
        savePayment(memberA, single, LocalDateTime.of(2026, 7, 22, 0, 0));

        // startDate=2026-07-01, endDate=2026-07-21 이 서비스에서 변환된 형태
        Page<AdminPaymentSummary> result = search(null, null,
                LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 22, 0, 0));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(AdminPaymentSummary::paidAt)
                .containsExactly(
                        LocalDateTime.of(2026, 7, 21, 23, 59),
                        LocalDateTime.of(2026, 7, 1, 0, 0));
    }

    @Test
    @DisplayName("REFUND 트랜잭션은 목록 행을 만들지 않는다")
    void search_ignoresRefundTransaction() {
        Payment payment = savePayment(memberA, single, LocalDateTime.of(2026, 7, 5, 10, 0));
        payment.addTransaction(Transaction.builder()
                .portoneTransactionId("tx_refund_" + SEQ.incrementAndGet())
                .payment(payment)
                .type(TransactionType.REFUND)
                .status(TransactionStatus.REFUNDED)
                .method(PaymentMethod.CARD)
                .pgProvider("TOSSPAYMENTS")
                .amount(4900L)
                .build());
        paymentRepository.saveAndFlush(payment);

        Page<AdminPaymentSummary> result = search(null, null, null, null);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).transactionStatus()).isEqualTo(TransactionStatus.PAID);
    }

    @Test
    @DisplayName("조건에 맞는 결제가 없으면 빈 페이지")
    void search_noMatch() {
        savePayment(memberA, single, LocalDateTime.of(2026, 7, 1, 10, 0));

        Page<AdminPaymentSummary> result = search("userB", null, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    private Page<AdminPaymentSummary> search(
            String nickname, Long productId, LocalDateTime from, LocalDateTime until
    ) {
        return paymentRepository.searchForAdmin(nickname, productId, from, until, PageRequest.of(0, 10));
    }

    private Member saveMember(String nickname) {
        return memberRepository.save(Member.builder()
                .clerkId("clerk_" + nickname)
                .name("테스트")
                .nickname(nickname)
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
    }

    private TicketProduct saveProduct(String name, Long price, int creditAmount) {
        return ticketProductRepository.save(TicketProduct.builder()
                .name(name)
                .price(price)
                .creditAmount(creditAmount)
                .active(true)
                .build());
    }

    private Payment savePayment(Member member, TicketProduct product, LocalDateTime paidAt) {
        return savePayment(member, List.of(product), paidAt);
    }

    private Payment savePayment(Member member, List<TicketProduct> products, LocalDateTime paidAt) {
        int seq = SEQ.incrementAndGet();
        long total = products.stream().mapToLong(TicketProduct::getPrice).sum();

        Payment payment = Payment.builder()
                .portonePaymentId("payment_" + seq)
                .member(member)
                .orderName("주문 " + seq)
                .amount(total)
                .currency("KRW")
                .build();

        payment.addTransaction(Transaction.builder()
                .portoneTransactionId("tx_" + seq)
                .payment(payment)
                .type(TransactionType.PAYMENT)
                .status(TransactionStatus.PAID)
                .method(PaymentMethod.CARD)
                .pgProvider("TOSSPAYMENTS")
                .amount(total)
                .paidAt(paidAt)
                .build());

        products.forEach(product -> payment.addPaymentTicket(PaymentTicket.builder()
                .payment(payment)
                .ticketProduct(product)
                .quantity(1)
                .unitPrice(product.getPrice())
                .unitCreditAmount(product.getCreditAmount())
                .build()));

        return paymentRepository.saveAndFlush(payment);
    }
}
