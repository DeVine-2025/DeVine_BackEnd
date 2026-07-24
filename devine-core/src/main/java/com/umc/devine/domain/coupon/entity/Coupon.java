package com.umc.devine.domain.coupon.entity;

import com.umc.devine.domain.coupon.enums.DiscountType;
import com.umc.devine.domain.ticket.entity.TicketProduct;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private Long discountValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicable_ticket_product_id")
    private TicketProduct applicableTicketProduct;

    @Column(name = "total_issue_limit")
    private Integer totalIssueLimit;

    @Column(name = "issued_count", nullable = false)
    private Integer issuedCount;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(length = 255)
    private String description;

    /**
     * 이미 보유한 쿠폰을 "사용"할 수 있는지 여부. 발급 수량 제한은 신규 발급 가능 여부({@link #isIssuable()})에만 관여하며,
     * 여기 포함시키면 캠페인이 한도까지 소진된 직후 이미 발급받은 회원까지 쿠폰을 못 쓰게 되는 버그가 생긴다.
     */
    public boolean isUsable() {
        if (!isActive) return false;
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(validFrom) && !now.isAfter(validUntil);
    }

    /** 신규 발급(코드 생성 포함) 가능 여부. 사용 가능 조건에 발급 수량 제한까지 추가로 확인한다. */
    public boolean isIssuable() {
        return isUsable() && (totalIssueLimit == null || issuedCount < totalIssueLimit);
    }

    public boolean isExpiringSoon() {
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(validUntil) && now.plusDays(7).isAfter(validUntil);
    }

    public boolean isApplicableTo(Long ticketProductId) {
        return applicableTicketProduct == null || applicableTicketProduct.getId().equals(ticketProductId);
    }

    public void update(String name, LocalDateTime validFrom, LocalDateTime validUntil,
                        Integer totalIssueLimit, boolean clearTotalIssueLimit,
                        Boolean isActive, String description) {
        if (name != null) this.name = name;
        if (validFrom != null) this.validFrom = validFrom;
        if (validUntil != null) this.validUntil = validUntil;
        if (clearTotalIssueLimit) {
            this.totalIssueLimit = null;
        } else if (totalIssueLimit != null) {
            this.totalIssueLimit = totalIssueLimit;
        }
        if (isActive != null) this.isActive = isActive;
        if (description != null) this.description = description;
    }
}
