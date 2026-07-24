package com.umc.devine.domain.coupon.service.query;

import com.umc.devine.domain.coupon.converter.CouponConverter;
import com.umc.devine.domain.coupon.dto.CouponResDTO;
import com.umc.devine.domain.coupon.entity.MemberCoupon;
import com.umc.devine.domain.coupon.exception.CouponException;
import com.umc.devine.domain.coupon.exception.code.CouponErrorReason;
import com.umc.devine.domain.coupon.helper.CouponDiscountCalculator;
import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponQueryServiceImpl implements CouponQueryService {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponDiscountCalculator couponDiscountCalculator;

    @Override
    public CouponResDTO.MemberCouponListDTO getMyCoupons(Member member) {
        List<MemberCoupon> memberCoupons = memberCouponRepository.findByMemberOrderByCreatedAtDesc(member);
        return CouponConverter.toMemberCouponListDTO(memberCoupons);
    }

    @Override
    public CouponResDTO.PreviewDTO preview(Long memberCouponId, Long originalAmount, List<Long> ticketProductIds, Member member) {
        MemberCoupon memberCoupon = memberCouponRepository.findByIdAndMember(memberCouponId, member)
                .orElseThrow(() -> new CouponException(CouponErrorReason.MEMBER_COUPON_NOT_FOUND));

        if (!memberCoupon.isAvailable()) {
            throw new CouponException(CouponErrorReason.COUPON_ALREADY_USED);
        }

        CouponDiscountCalculator.DiscountResult result =
                couponDiscountCalculator.calculate(originalAmount, memberCoupon.getCoupon(), ticketProductIds);

        return new CouponResDTO.PreviewDTO(originalAmount, result.discountAmount(), result.finalAmount());
    }
}
