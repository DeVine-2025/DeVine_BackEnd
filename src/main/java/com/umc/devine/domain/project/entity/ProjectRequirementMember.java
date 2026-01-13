package com.umc.devine.domain.project.entity;

import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "project_requirement_member")
public class ProjectRequirementMember extends BaseEntity {

    @EmbeddedId
    private ProjectRequirementMemberId id;

    @MapsId("projectId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "req_mem_num", nullable = false)
    private Integer requirementNum;

    @Column(name = "current_count", nullable = true)
    @Builder.Default
    private Integer currentCount = 0;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ProjectRequirementMemberId implements Serializable {
        private Long projectId;

        @Enumerated(EnumType.STRING)
        @Column(name = "req_mem_part", nullable = false)
        private ProjectPart part;
    }

}