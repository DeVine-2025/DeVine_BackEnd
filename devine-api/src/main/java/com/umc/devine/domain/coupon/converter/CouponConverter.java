package com.umc.devine.domain.coupon.converter;

import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.coupon.entity.MemberCoupon;

import java.util.List;

public class CouponConverter {

    public static CouponResDTO.MemberCouponDTO toMemberCouponDTO(MemberCoupon memberCoupon) {
        return new CouponResDTO.MemberCouponDTO(
                memberCoupon.getId(),
                memberCoupon.getCoupon().getName(),
                memberCoupon.getCoupon().getDiscountType(),
                memberCoupon.getCoupon().getDiscountValue(),
                memberCoupon.getCoupon().getApplicableTicketProduct() != null
                        ? memberCoupon.getCoupon().getApplicableTicketProduct().getId() : null,
                memberCoupon.getStatus(),
                memberCoupon.getUsedAt(),
                memberCoupon.getCreatedAt()
        );
    }

    public static CouponResDTO.MemberCouponListDTO toMemberCouponListDTO(List<MemberCoupon> memberCoupons) {
        return new CouponResDTO.MemberCouponListDTO(
                memberCoupons.stream().map(CouponConverter::toMemberCouponDTO).toList()
        );
    }
}
