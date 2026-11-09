package com.umc.devine.domain.ticket.helper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CreditRefundCalculator {

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

        return new Result(Math.max(remainingCredits, 0), refundable, expired, left, refundAmount, allocations);
    }

    private static boolean isExpired(Purchase purchase, LocalDateTime now) {
        return !now.isBefore(purchase.paidAt().plusYears(VALIDITY_YEARS));
    }

    private static long refundAmountOf(Purchase purchase, int credits) {
        if (credits == purchase.grantedCredits() && !purchase.hasRefundHistory()) {
            return purchase.amount();
        }
        return credits * (purchase.amount() / purchase.grantedCredits());
    }

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
