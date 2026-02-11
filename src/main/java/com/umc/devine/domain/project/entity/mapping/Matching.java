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

/**
 * partial unique index : uk_matching_active 확인
 * JPA는 @Index나 @UniqueConstraint는 WHERE 조건을 지원하지 않아서 부분 인덱스를 직접 매핑할 수 없음
 * 따라서, 부분 인덱스는 데이터베이스 수준에서 수동으로 생성 (추후 Flyway 등 마이그레이션 도구 사용 예정)
 */
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Matching extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "matching_type", nullable = false, length = 20)
    private MatchingType matchingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchingDecision decision = MatchingDecision.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "part", length = 20)
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

    public void changePart(ProjectPart part) {
        validatePendingStatus();
        this.part = part;
    }
}
