package com.umc.devine.domain.payment.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.enums.MemberCouponStatus;
import com.umc.devine.domain.coupon.repository.CouponRepository;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.payment.dto.PaymentReqDTO;
import com.umc.devine.domain.payment.dto.PaymentResDTO;
import com.umc.devine.domain.payment.exception.PaymentException;
import com.umc.devine.domain.payment.exception.code.PaymentErrorReason;
import com.umc.devine.domain.payment.repository.PaymentRepository;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.domain.ticket.repository.TicketProductRepository;
import com.umc.devine.infrastructure.portone.PortOneClient;
import com.umc.devine.infrastructure.portone.dto.PortOnePaymentResponse;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentCommandServiceCouponTest extends IntegrationTestSupport {

    @Autowired
    private PaymentCommandService paymentCommandService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TicketProductRepository ticketProductRepository;

    @Autowired
    private MemberReportCreditRepository memberReportCreditRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PortOneClient portOneClient;

    private Member member;
    private TicketProduct product;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .clerkId("payment-coupon-member")
                .nickname("pay-coupon-member")
                .used(MemberStatus.ACTIVE)
                .mainType(MemberMainType.DEVELOPER)
                .build());

        product = ticketProductRepository.save(TicketProduct.builder()
                .name("리포트 생성권 1개").price(4900L).creditAmount(1).active(true).build());
    }

    @AfterEach
    void tearDown() {
        memberCouponRepository.deleteAll();
        paymentRepository.deleteAll();
        couponRepository.deleteAll();
        ticketProductRepository.deleteAll();
        memberReportCreditRepository.deleteAll();
        memberRepository.deleteAll();
    }

    private Coupon saveCoupon(DiscountType type, long value) {
        return couponRepository.save(Coupon.builder()
                .name("결제 쿠폰")
                .discountType(type)
                .discountValue(value)
                .issuedCount(1)
                .usedCount(0)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validUntil(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build());
    }

    private PaymentReqDTO.CompletePaymentDTO completeReq(long amount, Long memberCouponId) {
        return new PaymentReqDTO.CompletePaymentDTO(
                "payment_test_1", "리포트 생성권 1개 x1", amount,
                List.of(new PaymentReqDTO.TicketPurchaseItem(product.getId(), 1)),
                memberCouponId
        );
    }

    private PortOnePaymentResponse paidResponse(long amount, String customData) {
        return new PortOnePaymentResponse(
                "txn_1", "PAID", new PortOnePaymentResponse.AmountDetail(amount),
                "KRW", "2026-07-24T00:00:00Z",
                new PortOnePaymentResponse.MethodDetail("PaymentMethodEasyPay", null, null, null, "KAKAOPAY", null),
                "KAKAOPAY", customData
        );
    }

    @Nested
    @DisplayName("completePayment - 쿠폰 적용")
    class CompletePaymentWithCoupon {

        @Test
        @DisplayName("정액 쿠폰 적용 후 할인된 금액으로 결제가 완료되고 쿠폰이 사용 처리된다")
        void completesWithDiscount() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            given(portOneClient.getPayment(anyString())).willReturn(paidResponse(3900L, null));

            PaymentResDTO.PaymentDTO result = paymentCommandService.completePayment(
                    completeReq(4900L, memberCoupon.getId()), member);

            assertThat(result.amount()).isEqualTo(3900L);

            MemberCoupon reloaded = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(MemberCouponStatus.USED);

            Coupon reloadedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
            assertThat(reloadedCoupon.getUsedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("PortOne 실결제 금액이 할인 후 금액과 다르면 예외가 발생하고 쿠폰은 사용되지 않는다")
        void throwsWhenActualAmountMismatchesDiscountedAmount() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            // PortOne이 할인 미반영 금액(4900)을 돌려준다고 응답 — 실제로는 3900이어야 함
            given(portOneClient.getPayment(anyString())).willReturn(paidResponse(4900L, null));

            assertThatThrownBy(() -> paymentCommandService.completePayment(
                    completeReq(4900L, memberCoupon.getId()), member))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getReason())
                            .isEqualTo(PaymentErrorReason.AMOUNT_MISMATCH));

            MemberCoupon reloaded = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(MemberCouponStatus.AVAILABLE);
        }

        @Test
        @DisplayName("쿠폰 없이 결제하면 원금 그대로 결제된다")
        void completesWithoutCoupon() {
            given(portOneClient.getPayment(anyString())).willReturn(paidResponse(4900L, null));

            PaymentResDTO.PaymentDTO result = paymentCommandService.completePayment(completeReq(4900L, null), member);

            assertThat(result.amount()).isEqualTo(4900L);
        }
    }

    @Nested
    @DisplayName("freePayment")
    class FreePayment {

        @Test
        @DisplayName("쿠폰 할인으로 0원이 되면 PortOne 호출 없이 결제가 완료되고 쿠폰이 사용 처리된다")
        void completesFreePayment() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 4900);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            PaymentReqDTO.FreePaymentDTO request = new PaymentReqDTO.FreePaymentDTO(
                    "리포트 생성권 1개 x1", 4900L,
                    List.of(new PaymentReqDTO.TicketPurchaseItem(product.getId(), 1)),
                    memberCoupon.getId());

            PaymentResDTO.PaymentDTO result = paymentCommandService.freePayment(request, member);

            assertThat(result.amount()).isZero();
            assertThat(result.paymentId()).startsWith("FREE_");
            verify(portOneClient, never()).getPayment(anyString());

            MemberCoupon reloaded = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(MemberCouponStatus.USED);
        }

        @Test
        @DisplayName("할인 후 금액이 0원이 아니면 예외가 발생한다")
        void throwsWhenAmountNotZero() {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            PaymentReqDTO.FreePaymentDTO request = new PaymentReqDTO.FreePaymentDTO(
                    "리포트 생성권 1개 x1", 4900L,
                    List.of(new PaymentReqDTO.TicketPurchaseItem(product.getId(), 1)),
                    memberCoupon.getId());

            assertThatThrownBy(() -> paymentCommandService.freePayment(request, member))
                    .isInstanceOf(PaymentException.class)
                    .satisfies(e -> assertThat(((PaymentException) e).getReason())
                            .isEqualTo(PaymentErrorReason.FREE_PAYMENT_AMOUNT_NOT_ZERO));
        }
    }

    @Nested
    @DisplayName("handleWebhookPayment - 쿠폰 적용")
    class WebhookWithCoupon {

        @Test
        @DisplayName("customData의 memberCouponId를 반영해 할인된 금액으로 검증하고 쿠폰을 사용 처리한다")
        void processesWebhookWithCoupon() throws Exception {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            String customData = objectMapper.writeValueAsString(Map.of(
                    "memberId", member.getId(),
                    "orderName", "리포트 생성권 1개 x1",
                    "items", List.of(Map.of("ticketProductId", product.getId(), "quantity", 1)),
                    "memberCouponId", memberCoupon.getId()
            ));
            given(portOneClient.getPayment("webhook_payment_1")).willReturn(paidResponse(3900L, customData));

            paymentCommandService.handleWebhookPayment("webhook_payment_1");

            assertThat(paymentRepository.existsByPortonePaymentId("webhook_payment_1")).isTrue();
            MemberCoupon reloaded = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(MemberCouponStatus.USED);
        }

        @Test
        @DisplayName("금액이 할인 후 금액과 다르면 결제를 저장하지 않는다")
        void skipsWhenAmountMismatches() throws Exception {
            Coupon coupon = saveCoupon(DiscountType.FIXED_AMOUNT, 1000);
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));

            String customData = objectMapper.writeValueAsString(Map.of(
                    "memberId", member.getId(),
                    "orderName", "리포트 생성권 1개 x1",
                    "items", List.of(Map.of("ticketProductId", product.getId(), "quantity", 1)),
                    "memberCouponId", memberCoupon.getId()
            ));
            // 할인 미반영 금액(4900)으로 응답 — 실제로는 3900이어야 함
            given(portOneClient.getPayment("webhook_payment_2")).willReturn(paidResponse(4900L, customData));

            paymentCommandService.handleWebhookPayment("webhook_payment_2");

            assertThat(paymentRepository.existsByPortonePaymentId("webhook_payment_2")).isFalse();
            MemberCoupon reloaded = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(MemberCouponStatus.AVAILABLE);
        }
    }
}
