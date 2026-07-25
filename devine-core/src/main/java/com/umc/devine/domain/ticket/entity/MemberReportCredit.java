package com.umc.devine.domain.ticket.entity;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "member_report_credit")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MemberReportCredit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_report_credit_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "remaining_count", nullable = false)
    private Integer remainingCount;

    public static MemberReportCredit of(Member member, int initialCount) {
        return MemberReportCredit.builder()
                .member(member)
                .remainingCount(initialCount)
                .build();
    }

    public void addCredits(int amount) {
        this.remainingCount += amount;
    }

    /**
     * 지급분을 회수하되 잔액이 부족하면 0에서 멈춘다(음수 방지).
     * @return 실제로 회수된 크레딧 수
     */
    public int revokeUpTo(int amount) {
        int revoked = Math.min(this.remainingCount, amount);
        this.remainingCount -= revoked;
        return revoked;
    }

    public void useCredit() {
        if (this.remainingCount <= 0) {
            throw new TicketException(TicketErrorReason.INSUFFICIENT_CREDITS);
        }
        this.remainingCount--;
    }

    public boolean hasCredits() {
        return this.remainingCount > 0;
    }
}
