package com.umc.devine.admin.member.entity;

import com.umc.devine.admin.member.enums.MemberStatusAction;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "member_status_history")
public class MemberStatusHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_status_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private MemberStatusAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "notify_requested", nullable = false)
    private boolean notifyRequested;

    @Column(name = "scheduled_withdrawal_at")
    private LocalDateTime scheduledWithdrawalAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_member_id")
    private Member processor;
}
