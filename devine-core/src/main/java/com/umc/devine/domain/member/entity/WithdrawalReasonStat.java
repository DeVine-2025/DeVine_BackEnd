package com.umc.devine.domain.member.entity;

import com.umc.devine.domain.member.enums.WithdrawalReason;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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
