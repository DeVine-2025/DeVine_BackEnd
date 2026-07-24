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
        // 동일한 코드를 여러 요청이 동시에 등록하면 두 요청 모두 UNREDEEMED를 읽고 각자 보유 쿠폰을 만들 수 있어
        // SELECT FOR UPDATE로 잠그고 재확인한다.
        CouponCode couponCode = couponCodeRepository.findByCodeWithLock(code.trim().toUpperCase())
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
