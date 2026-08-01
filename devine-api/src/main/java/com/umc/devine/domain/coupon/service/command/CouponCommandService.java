package com.umc.devine.domain.coupon.service.command;

import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.member.entity.Member;

public interface CouponCommandService {

    CouponResDTO.MemberCouponDTO registerByCode(String code, Member member);
}
