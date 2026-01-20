package com.umc.devine.domain.project.entity;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    @Column(name = "project_eta", nullable = true)
    @Builder.Default
    private LocalDate eta = null;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
