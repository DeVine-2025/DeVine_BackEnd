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

    /**
     * 이 신고 처리로 연동 조치(프로젝트 비노출, 계정 정지 등)가 실행된 적이 있는지 나타내는 <b>실행 이력</b>이다.
     *
     * <p>현재 상태의 거울이 아니다. 조치 실행 후 관리자가 노출 관리 API로 프로젝트를 다시 노출시켜도
     * "이 신고 처리로 조치가 실행됐다"는 사실은 변하지 않으므로 true로 남는다. 프로젝트의 현재 노출 상태는
     * {@code project.is_hidden}으로 판단해야 한다.
     *
     * <p>해제 경로를 두지 않은 이유: 한 프로젝트에 여러 신고가 달릴 수 있는데 어떤 신고가 비노출을 유발했는지
     * 추적하는 연결이 없어, 해제를 자동화하면 다른 신고의 제재까지 함께 풀릴 수 있다.
     */
    @Column(name = "linked_action_completed", nullable = false)
    @Builder.Default
    private boolean linkedActionCompleted = false;

    // 연동 조치가 실행된 시각
    @Column(name = "linked_action_at")
    private LocalDateTime linkedActionAt;

    public void markLinkedActionCompleted(LocalDateTime completedAt) {
        this.linkedActionCompleted = true;
        this.linkedActionAt = completedAt;
    }

    public void updateStatus(ComplaintStatus status, ComplaintAction action, String resolutionReason, Member resolver, LocalDateTime resolvedAt) {
        this.status = status;
        this.action = action;
        this.resolutionReason = resolutionReason;
        this.resolver = resolver;
        this.resolvedAt = resolvedAt;
    }
}
