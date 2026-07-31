package com.umc.devine.domain.ticket.entity;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.enums.CreditRefundStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원 자진 탈퇴 시 잔여 리포트 생성권에 대한 환불 신청 레코드. 실제 환불 처리는 관리자 결제관리 페이지에서 별도로 진행한다.
 * 금전 청구 기록이라 회원이 하드삭제되어도 행 자체는 삭제하지 않는다. detachMember() 참고.
 */
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

    /** 회원 하드삭제 시 detachMember()로 null이 될 수 있다. 그 뒤로는 조회 응답에서 회원을 특정할 수 없다. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
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

    /** 관리자 미처리 상태로 하드삭제 유예기간이 만료됐을 때 호출한다. 이미 처리완료된 건은 그대로 둔다. */
    public void expire() {
        if (this.status != CreditRefundStatus.REQUESTED) {
            return;
        }
        this.status = CreditRefundStatus.EXPIRED;
        this.processedAt = LocalDateTime.now();
    }

    /** 회원 하드삭제 시 FK를 끊는다. 청구 금액, 상태 등 감사 기록은 행에 그대로 남는다. */
    public void detachMember() {
        this.member = null;
    }
}
