package com.umc.devine.domain.coupon.dto;

import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.coupon.enums.MemberCouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public class CouponResDTO {

    @Schema(description = "보유 쿠폰 정보")
    public record MemberCouponDTO(
            Long memberCouponId,
            String couponName,
            DiscountType discountType,
            Long discountValue,
            Long applicableTicketProductId,
            MemberCouponStatus status,
            LocalDateTime usedAt,
            LocalDateTime issuedAt,
            LocalDateTime validUntil,

            @Schema(description = "지금 결제에 사용할 수 있는 쿠폰인지 (보유 상태 AVAILABLE + 쿠폰 자체가 활성/기간 내)")
            boolean isUsable
    ) {}

    @Schema(description = "보유 쿠폰 목록")
    public record MemberCouponListDTO(
            List<MemberCouponDTO> coupons
    ) {}

    @Schema(description = "결제 할인 미리보기")
    public record PreviewDTO(
            Long originalAmount,
            Long discountAmount,
            Long finalAmount
    ) {}
}
