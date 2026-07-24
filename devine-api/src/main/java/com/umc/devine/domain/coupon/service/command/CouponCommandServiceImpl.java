package com.umc.devine.domain.coupon.service.command;

import com.umc.devine.domain.coupon.converter.CouponConverter;
import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.coupon.entity.Coupon;
import com.umc.devine.domain.coupon.entity.CouponCode;
import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.coupon.repository.CouponCodeRepository;
import com.umc.devine.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponCommandServiceImpl implements CouponCommandService {

    private final CouponCodeRepository couponCodeRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Override
    public CouponResDTO.MemberCouponDTO registerByCode(String code, Member member) {
        CouponCode couponCode = couponCodeRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new CouponException(CouponErrorReason.COUPON_CODE_NOT_FOUND));

        if (!couponCode.isRedeemable()) {
            throw new CouponException(CouponErrorReason.COUPON_CODE_ALREADY_REDEEMED);
        }

        Coupon coupon = couponCode.getCoupon();
        if (!coupon.isUsable()) {
            throw new CouponException(CouponErrorReason.COUPON_NOT_USABLE);
        }

        MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issueTo(member, coupon));
        couponCode.redeem(memberCoupon);

        return CouponConverter.toMemberCouponDTO(memberCoupon);
    }
}
