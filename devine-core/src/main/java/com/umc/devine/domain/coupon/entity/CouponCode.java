package com.umc.devine.domain.coupon.entity;

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

    /** null이면 무제한, N이면 서로 다른 회원 N명까지 각 1회씩 이 코드를 등록할 수 있다. */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private int usedCount = 0;

    public static CouponCode of(Coupon coupon, String code, Integer maxUses) {
        return CouponCode.builder()
                .coupon(coupon)
                .code(code)
                .maxUses(maxUses)
                .usedCount(0)
                .build();
    }

    public void incrementUsedCount() {
        this.usedCount++;
    }

    public boolean isRedeemable() {
        return this.maxUses == null || this.usedCount < this.maxUses;
    }
}
