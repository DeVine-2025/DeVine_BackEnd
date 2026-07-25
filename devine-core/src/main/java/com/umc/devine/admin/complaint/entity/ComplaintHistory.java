package com.umc.devine.admin.complaint.entity;

import com.umc.devine.admin.complaint.enums.ComplaintAction;
import com.umc.devine.admin.complaint.enums.ComplaintStatus;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "complaint_history")
public class ComplaintHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ComplaintStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 20)
    private ComplaintAction action;

    @Column(name = "resolution_reason", columnDefinition = "TEXT")
    private String resolutionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolver_member_id")
    private Member resolver;
}
