package com.umc.devine.domain.ticket.entity;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.ticket.exception.TicketException;
import com.umc.devine.domain.ticket.exception.code.TicketErrorReason;
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
