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

    public boolean isUsable() {
        if (!isActive) return false;
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(validFrom) || now.isAfter(validUntil)) return false;
        return totalIssueLimit == null || issuedCount < totalIssueLimit;
    }

    public boolean isExpiringSoon() {
        return LocalDateTime.now().plusDays(7).isAfter(validUntil);
    }

    public boolean isApplicableTo(Long ticketProductId) {
        return applicableTicketProduct == null || applicableTicketProduct.getId().equals(ticketProductId);
    }
}
