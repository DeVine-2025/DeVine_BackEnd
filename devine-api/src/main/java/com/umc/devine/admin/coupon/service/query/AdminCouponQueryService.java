package com.umc.devine.admin.coupon.service.query;

import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.dto.PageRequest;

import java.util.List;

public interface AdminCouponQueryService {

    PagedResponse<AdminCouponResDTO.CouponDTO> getCoupons(PageRequest pageRequest);

    AdminCouponResDTO.CouponDTO getCoupon(Long couponId);

    List<AdminCouponResDTO.UsageStatDTO> getUsageStats(Long couponId);
}
