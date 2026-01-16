package com.umc.devine.domain.project.entity;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.enums.ProjectMode;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    @Column(name = "project_content", nullable = false, length = 255)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false)
    private ProjectStatus status;

    @Column(name = "project_start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "project_eta")
    private LocalDate eta;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_field", nullable = false, length = 20)
    private ProjectField projectField;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_mode", nullable = false, length = 20)
    private ProjectMode mode;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

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

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectRequirementMember> requirements = new ArrayList<>();

    public String getTitle() {
        return this.name;
    }

    public void updateProjectInfo(ProjectField projectField,
                                  Category category,
                                  ProjectMode mode,
                                  Integer durationMonths,
                                  String location,
                                  LocalDate recruitmentDeadline,
                                  LocalDate startDate) {
        this.projectField = projectField;
        this.category = category;
        this.mode = mode;
        this.durationMonths = durationMonths;
        this.location = location;
        this.recruitmentDeadline = recruitmentDeadline;
        this.startDate = startDate;
        this.eta = startDate.plusMonths(durationMonths);
    }

    public void updateContent(String title, String content) {
        this.name = title;
        this.content = content;
    }

    public void delete() {
        this.status = ProjectStatus.DELETED;
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
}