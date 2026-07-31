package com.umc.devine.admin.complaint.entity;

import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.admin.complaint.enums.ComplaintTargetType;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "complaint")
public class Complaint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complainant_member_id", nullable = false)
    private Member complainant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "respondent_member_id", nullable = false)
    private Member respondentMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ComplaintTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    private ComplaintAction action;

    @Column(name = "resolution_reason", columnDefinition = "TEXT")
    private String resolutionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolver_member_id")
    private Member resolver;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public void updateStatus(ComplaintStatus status, ComplaintAction action, String resolutionReason, Member resolver, LocalDateTime resolvedAt) {
        this.status = status;
        this.action = action;
        this.resolutionReason = resolutionReason;
        this.resolver = resolver;
        this.resolvedAt = resolvedAt;
    }
}
