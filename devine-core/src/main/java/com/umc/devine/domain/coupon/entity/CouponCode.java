package com.umc.devine.domain.coupon.entity;

import com.umc.devine.domain.coupon.enums.CouponCodeStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupon_code")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CouponCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_code_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponCodeStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_member_coupon_id")
    private MemberCoupon redeemedMemberCoupon;

    public static CouponCode of(Coupon coupon, String code) {
        return CouponCode.builder()
                .coupon(coupon)
                .code(code)
                .status(CouponCodeStatus.UNREDEEMED)
                .build();
    }

    public void redeem(MemberCoupon memberCoupon) {
        this.status = CouponCodeStatus.REDEEMED;
        this.redeemedMemberCoupon = memberCoupon;
    }

    public boolean isRedeemable() {
        return this.status == CouponCodeStatus.UNREDEEMED;
    }
}
