package com.umc.devine.domain.coupon.entity;

import com.umc.devine.domain.coupon.enums.MemberCouponStatus;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.payment.entity.Payment;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_coupon")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MemberCoupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_coupon_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberCouponStatus status;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    public static MemberCoupon issueTo(Member member, Coupon coupon) {
        return MemberCoupon.builder()
                .member(member)
                .coupon(coupon)
                .status(MemberCouponStatus.AVAILABLE)
                .build();
    }

    public void use(Payment payment) {
        this.status = MemberCouponStatus.USED;
        this.usedAt = LocalDateTime.now();
        this.payment = payment;
    }

    public boolean isAvailable() {
        return this.status == MemberCouponStatus.AVAILABLE;
    }
}
