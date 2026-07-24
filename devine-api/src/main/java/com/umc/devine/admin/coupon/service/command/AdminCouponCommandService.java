package com.umc.devine.admin.coupon.service.command;

import com.umc.devine.admin.coupon.dto.AdminCouponReqDTO;
import com.umc.devine.admin.coupon.dto.AdminCouponResDTO;

public interface AdminCouponCommandService {

    AdminCouponResDTO.CouponDTO createCoupon(AdminCouponReqDTO.CreateCouponReq request);

    AdminCouponResDTO.CouponDTO updateCoupon(Long couponId, AdminCouponReqDTO.UpdateCouponReq request);

    AdminCouponResDTO.IssueResultDTO issueCoupon(Long couponId, AdminCouponReqDTO.IssueCouponReq request);
}
