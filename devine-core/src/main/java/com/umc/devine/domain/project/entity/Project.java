package com.umc.devine.domain.project.entity;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "project")
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @Column(name = "project_name", nullable = false, length = 255)
    private String name;

    @Column(name = "project_content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false, length = 20)
    private ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_field", nullable = false, length = 20)
    private ProjectField projectField;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_mode", nullable = false, length = 20)
    private ProjectMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_range", nullable = false, length = 20)
    private DurationRange durationRange;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "recruitment_deadline", nullable = false)
    private LocalDate recruitmentDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 전체 조회수 (누적)
    @Column(name = "total_view_count", nullable = false)
    @Builder.Default
    private Long totalViewCount = 0L;

    // 이번 주 조회수 (집계 중, 매주 월요일에 previousWeekViewCount로 이동 후 리셋)
    @Column(name = "weekly_view_count", nullable = false)
    @Builder.Default
    private Long weeklyViewCount = 0L;

    // 지난 주 조회수 (표시용, 매주 월요일에 weeklyViewCount 값으로 업데이트)
    @Column(name = "previous_week_view_count", nullable = false)
    @Builder.Default
    private Long previousWeekViewCount = 0L;

    // 마지막 조회수 리셋 날짜 (월요일 자정)
    @Column(name = "last_view_reset_date")
    private LocalDate lastViewResetDate;

    // 세 출처의 OR 계산값이다. recomputeHidden()으로만 갱신한다.
    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private boolean hidden = false;

    @Column(name = "hidden_by_owner", nullable = false)
    @Builder.Default
    private boolean hiddenByOwner = false;

    @Column(name = "hidden_by_admin", nullable = false)
    @Builder.Default
    private boolean hiddenByAdmin = false;

    @Column(name = "report_blinded", nullable = false)
    @Builder.Default
    private boolean reportBlinded = false;

    // 노출 상태를 마지막으로 변경한 처리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visibility_changed_by")
    private Member visibilityChangedBy;

    // 노출 상태를 마지막으로 변경한 시각
    @Column(name = "visibility_changed_at")
    private LocalDateTime visibilityChangedAt;

    // 마감 처리자. 비어 있으면 자동 마감(배치·마감 확인 훅)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private Member closedBy;

    // 마감 처리 시각
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectImage> images = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectRequirementMember> requirements = new ArrayList<>();

    public String getTitle() {
        return this.name;
    }

    public void updateProjectInfo(ProjectField projectField,
                                  Category category,
                                  ProjectMode mode,
                                  DurationRange durationRange,
                                  String location,
                                  LocalDate recruitmentDeadline) {
        this.projectField = projectField;
        this.category = category;
        this.mode = mode;
        this.durationRange = durationRange;
        this.location = location;
        this.recruitmentDeadline = recruitmentDeadline;
    }

    public void updateContent(String title, String content) {
        this.name = title;
        this.content = content;
    }

    public void delete() {
        this.status = ProjectStatus.DELETED;
    }

    public boolean changeVisibilityByOwner(boolean hide, Member processor, LocalDateTime changedAt) {
        this.hiddenByOwner = hide;
        return applyVisibilityChange(processor, changedAt);
    }

    public boolean changeVisibilityByAdmin(boolean visible, Member processor, LocalDateTime changedAt) {
        this.hiddenByAdmin = !visible;
        return applyVisibilityChange(processor, changedAt);
    }

    public boolean changeReportBlind(boolean blinded, LocalDateTime changedAt) {
        if (this.reportBlinded == blinded) {
            return false;
        }
        this.reportBlinded = blinded;
        return applyVisibilityChange(this.visibilityChangedBy, changedAt);
    }

    private boolean applyVisibilityChange(Member processor, LocalDateTime changedAt) {
        boolean before = this.hidden;
        recomputeHidden();
        this.visibilityChangedBy = processor;
        this.visibilityChangedAt = changedAt;
        return before != this.hidden;
    }

    private void recomputeHidden() {
        this.hidden = this.hiddenByOwner || this.hiddenByAdmin || this.reportBlinded;
    }

    public void updateStatus(ProjectStatus status) {
        this.status = status;
    }

    public void addImage(ProjectImage image) {
        this.images.add(image);
    }

    public void addRequirement(ProjectRequirementMember requirement) {
        this.requirements.add(requirement);
    }

    public void clearImages() {
        this.images.clear();
    }

    public void clearRequirements() {
        this.requirements.clear();
    }

    // ==================== 상태 전환 메서드 ====================

    public void startRecruiting() {
        this.status = ProjectStatus.RECRUITING;
        this.closedBy = null;
        this.closedAt = null;
    }

    public void startProgress() {
        this.status = ProjectStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = ProjectStatus.COMPLETED;
    }

    /** @param processor 수동 마감한 회원. 자동 마감이면 null */
    public void close(Member processor, LocalDateTime closedAt) {
        this.status = ProjectStatus.CLOSED;
        this.closedBy = processor;
        this.closedAt = closedAt;
    }

    public void markDeadlinePassed() {
        this.status = ProjectStatus.DEADLINE_PASSED;
    }

    /**
     * 매칭 이력 때문에 행을 남겨야 하는 삭제: 이름만 남기고 내용을 비운다.
     * 이미지·모집 정보 등 연관 행은 호출하는 쪽에서 지운다.
     */
    public void eraseForDeletion() {
        this.content = "";
        this.location = "";
        this.status = ProjectStatus.DELETED;
        this.hiddenByOwner = true;
        recomputeHidden();
    }

    // ==================== 도메인 검증 메서드 ====================

    /** 모집 단계(모집 중·마감일 지남)인지. 이 단계를 벗어나려면 대기 건을 모두 처리해야 한다. */
    public boolean isInRecruitingPhase() {
        return this.status == ProjectStatus.RECRUITING || this.status == ProjectStatus.DEADLINE_PASSED;
    }

    public boolean isHiddenByModeration() {
        return this.hiddenByAdmin || this.reportBlinded;
    }

    public boolean isRecruiting() {
        return this.status == ProjectStatus.RECRUITING;
    }

    public boolean isAllRequirementsFulfilled() {
        if (this.requirements.isEmpty()) {
            return false;
        }
        return this.requirements.stream().allMatch(ProjectRequirementMember::isFulfilled);
    }

    public boolean isInProgress() {
        return this.status == ProjectStatus.IN_PROGRESS;
    }

    public boolean isOwnedBy(Member member) {
        return this.member.getId().equals(member.getId());
    }
}