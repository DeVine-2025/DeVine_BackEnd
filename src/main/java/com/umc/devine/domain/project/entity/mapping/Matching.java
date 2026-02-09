package com.umc.devine.domain.project.entity.mapping;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorCode;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(
        name = "matching",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_matching_active",
                        columnNames = {"project_id", "member_id", "matching_type"}
                )
        }
)
public class Matching extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "matching_type", nullable = false)
    private MatchingType matchingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchingDecision decision = MatchingDecision.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "part")
    private ProjectPart part;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public void cancel() {
        this.status = MatchingStatus.CANCELLED;
    }

    public void accept() {
        validatePendingStatus();
        this.status = MatchingStatus.COMPLETED;
    }

    public void reject() {
        validatePendingStatus();
        this.status = MatchingStatus.CANCELLED;
    }

    public void applyDecision(MatchingDecision decision) {
        Objects.requireNonNull(decision, "decision must not be null");
        this.decision = decision;
        if (decision == MatchingDecision.ACCEPT) {
            accept();
        } else {
            reject();
        }
    }

    private void validatePendingStatus() {
        if (this.status != MatchingStatus.PENDING) {
            throw new MatchingException(MatchingErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    public boolean isApplyType() {
        return this.matchingType == MatchingType.APPLY;
    }

    public boolean isProposeType() {
        return this.matchingType == MatchingType.PROPOSE;
    }

    public boolean isTargetMember(Member member) {
        return this.member.getId().equals(member.getId());
    }

    public boolean isPending() {
        return this.status == MatchingStatus.PENDING;
    }
}
