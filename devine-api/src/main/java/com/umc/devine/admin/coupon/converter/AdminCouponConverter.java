package com.umc.devine.admin.coupon.converter;

import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;
import com.umc.devine.domain.coupon.entity.Coupon;

public class AdminCouponConverter {

    public static AdminCouponResDTO.CouponDTO toCouponDTO(Coupon coupon) {
        return new AdminCouponResDTO.CouponDTO(
                coupon.getId(),
                coupon.getName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getApplicableTicketProduct() != null ? coupon.getApplicableTicketProduct().getId() : null,
                coupon.getApplicableTicketProduct() != null ? coupon.getApplicableTicketProduct().getName() : null,
                coupon.getTotalIssueLimit(),
                coupon.getIssuedCount(),
                coupon.getUsedCount(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getIsActive(),
                coupon.getDescription(),
                coupon.getCreatedAt()
        );
    }

    public static AdminCouponResDTO.UsageStatDTO toUsageStatDTO(Coupon coupon) {
        int issued = coupon.getIssuedCount();
        double usageRate = issued == 0 ? 0.0 : (double) coupon.getUsedCount() / issued;
        return new AdminCouponResDTO.UsageStatDTO(
                coupon.getId(),
                coupon.getName(),
                coupon.getIssuedCount(),
                coupon.getUsedCount(),
                usageRate,
                coupon.isExpiringSoon(),
                coupon.getValidUntil()
        );
    }
}
