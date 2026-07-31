package com.umc.devine.domain.coupon.service.query;

import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.member.entity.Member;

import java.util.List;

public interface CouponQueryService {

    CouponResDTO.MemberCouponListDTO getMyCoupons(Member member);

    CouponResDTO.PreviewDTO preview(Long memberCouponId, Long originalAmount, List<Long> ticketProductIds, Member member);
}
