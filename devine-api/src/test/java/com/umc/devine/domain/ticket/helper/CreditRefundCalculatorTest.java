package com.umc.devine.domain.ticket.helper;

import com.umc.devine.domain.ticket.helper.CreditRefundCalculator.Purchase;
import com.umc.devine.domain.ticket.helper.CreditRefundCalculator.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 01-1 기능명세서 3장 "환불 계산 규칙"과 테스트 시나리오 T4~T6-2.
 */
class CreditRefundCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 9, 12, 0);

    private static Purchase purchase(long id, LocalDateTime paidAt, long amount, int granted, int refunded, boolean history) {
        return new Purchase(id, "pay_" + id, paidAt, amount, granted, refunded, history);
    }

    @Test
    @DisplayName("T4: 3개 묶음(9,900원) 사서 1개 사용, 구매 20일 뒤 → 환불 가능 2개 · 6,600원")
    void t4_bundlePartiallyUsed() {
        Result result = CreditRefundCalculator.calculate(2,
                List.of(purchase(1, NOW.minusDays(20), 9_900, 3, 0, false)), NOW);

        assertThat(result.refundableCredits()).isEqualTo(2);
        assertThat(result.refundAmount()).isEqualTo(6_600);
        assertThat(result.nonRefundableCredits()).isZero();
        assertThat(result.allocations()).singleElement()
                .satisfies(a -> {
                    assertThat(a.purchase().paymentId()).isEqualTo(1L);
                    assertThat(a.credits()).isEqualTo(2);
                    assertThat(a.amount()).isEqualTo(6_600);
                });
    }

    @Test
    @DisplayName("T5: 쿠폰 할인으로 3개 묶음을 7,900원에 사고 미사용 → 실제 결제 금액 7,900원 전액")
    void t5_couponDiscountFullRefund() {
        Result result = CreditRefundCalculator.calculate(3,
                List.of(purchase(1, NOW.minusDays(3), 7_900, 3, 0, false)), NOW);

        assertThat(result.refundableCredits()).isEqualTo(3);
        assertThat(result.refundAmount()).isEqualTo(7_900);
    }

    @Test
    @DisplayName("쿠폰 할인 후 일부만 환불하면 단가의 1원 미만은 버린다(7,900 ÷ 3 = 2,633)")
    void discountedPartialRefund_floorsUnitPrice() {
        Result result = CreditRefundCalculator.calculate(2,
                List.of(purchase(1, NOW.minusDays(3), 7_900, 3, 0, false)), NOW);

        assertThat(result.refundAmount()).isEqualTo(2 * 2_633);
    }

    @Test
    @DisplayName("T6: 1년 2개월 전에 산 생성권 1개 남음 → 환불 가능 0개 / 환불 불가 1개(만료)")
    void t6_expired() {
        Result result = CreditRefundCalculator.calculate(1,
                List.of(purchase(1, NOW.minusYears(1).minusMonths(2), 4_900, 1, 0, false)), NOW);

        assertThat(result.refundableCredits()).isZero();
        assertThat(result.expiredCredits()).isEqualTo(1);
        assertThat(result.nonRefundableCredits()).isEqualTo(1);
        assertThat(result.refundAmount()).isZero();
        assertThat(result.allocations()).isEmpty();
    }

    @Test
    @DisplayName("결제일로부터 정확히 1년이 되면 만료로 본다")
    void expiresExactlyAtOneYear() {
        Result result = CreditRefundCalculator.calculate(1,
                List.of(purchase(1, NOW.minusYears(1), 4_900, 1, 0, false)), NOW);

        assertThat(result.expiredCredits()).isEqualTo(1);
    }

    @Test
    @DisplayName("T6-1: 관리자 지급 1개 + 5일 전 1개 구매(4,900원), 1개 사용 → 무상분을 먼저 쓴 것으로 보고 환불 가능 1개 · 4,900원")
    void t6_1_freeCreditsUsedFirst() {
        Result result = CreditRefundCalculator.calculate(1,
                List.of(purchase(1, NOW.minusDays(5), 4_900, 1, 0, false)), NOW);

        assertThat(result.refundableCredits()).isEqualTo(1);
        assertThat(result.refundAmount()).isEqualTo(4_900);
        assertThat(result.freeCredits()).isZero();
    }

    @Test
    @DisplayName("T6-2: 3개 묶음 중 2개 부분 환불 완료, 남은 1개 → 환불 가능 1개 · 3,300원(이력이 있으면 개수 × 단가)")
    void t6_2_partialRefundHistory() {
        Result result = CreditRefundCalculator.calculate(1,
                List.of(purchase(1, NOW.minusDays(10), 9_900, 3, 2, true)), NOW);

        assertThat(result.refundableCredits()).isEqualTo(1);
        assertThat(result.refundAmount()).isEqualTo(3_300);
    }

    @Test
    @DisplayName("지급 개수 전부가 환불 가능해도 환불 이력이 있으면 전액이 아니라 개수 × 단가로 계산한다")
    void fullCountWithHistory_usesUnitPrice() {
        Result result = CreditRefundCalculator.calculate(3,
                List.of(purchase(1, NOW.minusDays(3), 7_900, 3, 0, true)), NOW);

        assertThat(result.refundAmount()).isEqualTo(3 * 2_633);
    }

    @Test
    @DisplayName("남은 생성권은 가장 최근 결제분부터 채우고, 유료분을 다 채우고 남는 개수는 무상분이다")
    void latestPurchaseFirst_thenFree() {
        Purchase older = purchase(1, NOW.minusDays(30), 9_900, 3, 0, false);
        Purchase newer = purchase(2, NOW.minusDays(1), 4_900, 1, 0, false);

        Result result = CreditRefundCalculator.calculate(6, List.of(older, newer), NOW);

        assertThat(result.refundableCredits()).isEqualTo(4);
        assertThat(result.freeCredits()).isEqualTo(2);
        assertThat(result.refundAmount()).isEqualTo(4_900 + 9_900);
        assertThat(result.allocations()).extracting(a -> a.purchase().paymentId())
                .containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("최근 결제분이 만료되지 않고 오래된 결제분만 만료됐으면, 최근분은 환불 가능하고 오래된 분은 만료로 센다")
    void mixedExpiry() {
        Purchase expired = purchase(1, NOW.minusYears(2), 4_900, 1, 0, false);
        Purchase valid = purchase(2, NOW.minusDays(1), 4_900, 1, 0, false);

        Result result = CreditRefundCalculator.calculate(2, List.of(expired, valid), NOW);

        assertThat(result.refundableCredits()).isEqualTo(1);
        assertThat(result.expiredCredits()).isEqualTo(1);
        assertThat(result.refundAmount()).isEqualTo(4_900);
    }

    @Test
    @DisplayName("이미 전부 환불된 결제 건은 건너뛴다(관리자 결제 전체 환불)")
    void fullyRefundedPurchaseSkipped() {
        Result result = CreditRefundCalculator.calculate(1,
                List.of(purchase(1, NOW.minusDays(3), 4_900, 1, 1, true)), NOW);

        assertThat(result.refundableCredits()).isZero();
        assertThat(result.freeCredits()).isEqualTo(1);
    }

    @Test
    @DisplayName("결제 기록이 없으면 남은 생성권은 모두 무상분이다")
    void noPurchases_allFree() {
        Result result = CreditRefundCalculator.calculate(2, List.of(), NOW);

        assertThat(result.remainingCredits()).isEqualTo(2);
        assertThat(result.freeCredits()).isEqualTo(2);
        assertThat(result.refundAmount()).isZero();
    }

    @Test
    @DisplayName("남은 생성권이 0이면 아무것도 환불하지 않는다")
    void zeroRemaining() {
        Result result = CreditRefundCalculator.calculate(0,
                List.of(purchase(1, NOW.minusDays(3), 4_900, 1, 0, false)), NOW);

        assertThat(result.refundableCredits()).isZero();
        assertThat(result.allocations()).isEmpty();
    }
}
