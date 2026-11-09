package com.umc.devine.domain.ticket.helper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 탈퇴 시 남은 생성권의 환불 가능 개수/금액을 추정한다(01-1 기능명세서 3장, 결정 A).
 *
 * <p>{@code member_report_credit.remaining_count}는 숫자 하나뿐이라 어느 결제분이 남았는지 알 수 없다.
 * 대표 결정(10-28)에 따라 무상분을 먼저 쓰고, 유료분은 먼저 산 것부터 쓴 것으로 본다.
 * 그러면 남은 생성권은 가장 최근에 산 유료분부터 남아 있는 것이 된다.
 *
 * <p>생성권 장부(04-2)가 생기면 장부 조회로 바꿀 수 있도록 계산을 이 함수 하나에 모아 둔다. DB에 접근하지 않는다.
 */
public final class CreditRefundCalculator {

    /** 약관 제12조 6항: 생성권 유효기간은 결제일로부터 1년. */
    static final int VALIDITY_YEARS = 1;

    private CreditRefundCalculator() {
    }

    /**
     * @param remainingCredits 회원의 남은 생성권 수
     * @param purchases        유료 결제 건(0원 결제 제외). 순서는 상관없다
     * @param now              기준 시각
     */
    public static Result calculate(int remainingCredits, List<Purchase> purchases, LocalDateTime now) {
        int left = Math.max(remainingCredits, 0);
        int refundable = 0;
        int expired = 0;
        long refundAmount = 0;
        List<Allocation> allocations = new ArrayList<>();

        List<Purchase> latestFirst = purchases.stream()
                .sorted(Comparator.comparing(Purchase::paidAt).thenComparing(Purchase::paymentId).reversed())
                .toList();

        for (Purchase purchase : latestFirst) {
            if (left == 0) {
                break;
            }
            int filled = Math.min(left, purchase.refundableCapacity());
            if (filled == 0) {
                continue;
            }
            left -= filled;

            if (isExpired(purchase, now)) {
                expired += filled;
                continue;
            }

            long amount = refundAmountOf(purchase, filled);
            refundable += filled;
            refundAmount += amount;
            allocations.add(new Allocation(purchase, filled, amount));
        }

        // 유료 결제 건을 모두 채우고도 남는 개수는 무상분(관리자 지급, 가입 지급, 생성 실패 반환 등)이다
        return new Result(Math.max(remainingCredits, 0), refundable, expired, left, refundAmount, allocations);
    }

    private static boolean isExpired(Purchase purchase, LocalDateTime now) {
        return !now.isBefore(purchase.paidAt().plusYears(VALIDITY_YEARS));
    }

    /**
     * 환불 단가 = 실제 결제 금액(쿠폰 할인 후) ÷ 지급 개수, 1원 미만은 버린다.
     * 지급 개수 전부가 환불 가능하고 환불 이력이 없을 때만 결제 금액 전액을 돌려준다(이중 환불 방지).
     */
    private static long refundAmountOf(Purchase purchase, int credits) {
        if (credits == purchase.grantedCredits() && !purchase.hasRefundHistory()) {
            return purchase.amount();
        }
        return credits * (purchase.amount() / purchase.grantedCredits());
    }

    /**
     * 유료 결제 건 하나.
     *
     * @param grantedCredits         지급 개수(quantity × unit_credit_amount의 합)
     * @param alreadyRefundedCredits 이미 환불했거나 환불 진행/신청 중인 개수
     * @param hasRefundHistory       FAILED가 아닌 환불 행이 하나라도 있는지
     */
    public record Purchase(
            Long paymentId,
            String portonePaymentId,
            LocalDateTime paidAt,
            long amount,
            int grantedCredits,
            int alreadyRefundedCredits,
            boolean hasRefundHistory
    ) {
        int refundableCapacity() {
            return Math.max(grantedCredits - alreadyRefundedCredits, 0);
        }
    }

    /** 환불 가능으로 계산된 결제 건별 수량/금액. */
    public record Allocation(Purchase purchase, int credits, long amount) {
    }

    /**
     * @param remainingCredits  남은 생성권 전체
     * @param refundableCredits 환불 가능
     * @param expiredCredits    유효기간 만료(환불 불가)
     * @param freeCredits       무상분(환불 불가)
     */
    public record Result(
            int remainingCredits,
            int refundableCredits,
            int expiredCredits,
            int freeCredits,
            long refundAmount,
            List<Allocation> allocations
    ) {
        public int nonRefundableCredits() {
            return expiredCredits + freeCredits;
        }
    }
}
