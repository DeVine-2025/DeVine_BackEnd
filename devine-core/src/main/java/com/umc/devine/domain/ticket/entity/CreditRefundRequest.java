package com.umc.devine.domain.ticket.entity;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 회원 자진 탈퇴 시 잔여 리포트 생성권에 대한 환불 신청 레코드. 실제 환불 처리는 관리자 결제관리 페이지에서 별도로 진행한다. */
@Entity
@Table(name = "credit_refund_request")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class CreditRefundRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credit_refund_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "credit_amount_at_request", nullable = false)
    private Integer creditAmountAtRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CreditRefundStatus status = CreditRefundStatus.REQUESTED;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_member_id")
    private Member processor;

    public static CreditRefundRequest of(Member member, int creditAmount) {
        return CreditRefundRequest.builder()
                .member(member)
                .creditAmountAtRequest(creditAmount)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    public void process(Member processor) {
        this.status = CreditRefundStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
        this.processor = processor;
    }
}
