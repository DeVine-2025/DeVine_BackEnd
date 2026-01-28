package com.umc.devine.domain.report.entity;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.enums.ReportVisibility;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "dev_report",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dev_report_git_repo_type",
                columnNames = {"git_repo_id", "report_type"}
        ))
public class DevReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dev_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "git_repo_id", nullable = false)
    private GitRepoUrl gitRepoUrl;

    @Column(name = "report_content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    @Builder.Default
    private ReportVisibility visibility = ReportVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public void updateVisibility(ReportVisibility visibility) {
        this.visibility = visibility;
    }

    public void updateErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void completeReport(String content) {
        this.content = content;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void failReport(String errorMessage) {
        this.errorMessage = errorMessage;
        this.completedAt = null;
    }
}
