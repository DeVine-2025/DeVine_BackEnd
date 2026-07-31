package com.umc.devine.global.scheduler.harddelete;

import com.umc.devine.domain.coupon.repository.MemberCouponRepository;
import com.umc.devine.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 쿠폰 지급/사용 내역은 환불 대상이 아니므로(REQ-WD-006) 그대로 삭제한다. */
@Component
@Order(20)
@RequiredArgsConstructor
public class MemberCouponHardDeleteHandler implements MemberHardDeleteHandler {

    private final MemberCouponRepository memberCouponRepository;

    @Override
    public void handle(Member member) {
        memberCouponRepository.bulkDeleteByMember(member);
    }
}
