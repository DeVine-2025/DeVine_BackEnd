package com.umc.devine.domain.techstack.entity.mapping;

import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "project_requirement_techstack")
public class ProjectRequirementTechstack extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_requirement_techstack_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_requirement_member_id", nullable = false)
    private ProjectRequirementMember requirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "techstack_id", nullable = false)
    private Techstack techstack;
}