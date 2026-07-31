package com.umc.devine.domain.coupon.helper;

import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CouponDiscountCalculator {

    public record DiscountResult(long discountAmount, long finalAmount) {}

    /**
     * 쿠폰 유효성(활성/기간)과 적용 대상 상품 범위를 검증한다.
     * applicableTicketProduct가 지정된 쿠폰은 구매 상품 목록에 해당 상품이 포함되어 있지 않으면 사용 자체를 거부한다.
     */
    public void validate(Coupon coupon, List<Long> ticketProductIds) {
        if (!coupon.isUsable()) {
            throw new CouponException(CouponErrorReason.COUPON_NOT_USABLE);
        }
        if (coupon.getApplicableTicketProduct() != null) {
            Long requiredId = coupon.getApplicableTicketProduct().getId();
            boolean included = ticketProductIds != null && ticketProductIds.contains(requiredId);
            if (!included) {
                throw new CouponException(CouponErrorReason.COUPON_NOT_APPLICABLE_PRODUCT);
            }
        }
    }

    public DiscountResult calculate(long originalAmount, Coupon coupon, List<Long> ticketProductIds) {
        validate(coupon, ticketProductIds);

        long discount = switch (coupon.getDiscountType()) {
            case FIXED_RATE -> originalAmount * coupon.getDiscountValue() / 100;
            case FIXED_AMOUNT -> coupon.getDiscountValue();
        };
        discount = Math.min(discount, originalAmount);

        return new DiscountResult(discount, originalAmount - discount);
    }
}
