package com.umc.devine.domain.project.entity;

import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.exception.MatchingException;
import com.umc.devine.domain.project.exception.code.MatchingErrorReason;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "project_requirement_member")
public class ProjectRequirementMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_requirement_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "req_mem_part", nullable = false, length = 30)
    private ProjectPart part;

    @Column(name = "req_mem_num", nullable = false)
    private Integer requirementNum;

    @Column(name = "current_count")
    @Builder.Default
    private Integer currentCount = 0;

    public void incrementCurrentCount() {
        if (isFulfilled()) {
            throw new MatchingException(MatchingErrorReason.PART_ALREADY_FULFILLED);
        }
        this.currentCount++;
    }

    public boolean isFulfilled() {
        return this.currentCount >= this.requirementNum;
    }
}
