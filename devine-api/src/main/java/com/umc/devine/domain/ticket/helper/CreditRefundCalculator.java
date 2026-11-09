package com.umc.devine.domain.ticket.helper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * remaining_count만으로는 어느 결제분이 남았는지 알 수 없어 추정한다.
 * 무상분을 먼저, 유료분은 먼저 산 것부터 쓴 것으로 본다. 생성권 장부가 생기면 이 클래스만 바꾸면 된다.
 */
public final class CreditRefundCalculator {

    /** 약관 제12조 6항: 생성권 유효기간은 결제일로부터 1년. */
    static final int VALIDITY_YEARS = 1;

    private CreditRefundCalculator() {
    }

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

    /** 지급 개수 전부가 환불 가능하고 환불 이력이 없을 때만 전액을 돌려준다(이중 환불 방지). */
    private static long refundAmountOf(Purchase purchase, int credits) {
        if (credits == purchase.grantedCredits() && !purchase.hasRefundHistory()) {
            return purchase.amount();
        }
        return credits * (purchase.amount() / purchase.grantedCredits());
    }

    /** alreadyRefundedCredits는 이미 환불했거나 진행/신청 중인 개수 */
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

    public record Allocation(Purchase purchase, int credits, long amount) {
    }

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
