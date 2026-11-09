package com.umc.devine.domain.member.entity;

import com.umc.devine.domain.member.enums.WithdrawalReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * 탈퇴 사유 통계(FUNC-MEM-003 5번).
 * 회원과 연결하지 않도록 member_id를 두지 않고, BaseEntity도 상속하지 않는다(created_by에 clerk_id가 들어가므로).
 */
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "withdrawal_reason_stat")
public class WithdrawalReasonStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_reason_stat_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private WithdrawalReason reason;

    @Column(name = "withdrawn_on", nullable = false)
    private LocalDate withdrawnOn;
}
