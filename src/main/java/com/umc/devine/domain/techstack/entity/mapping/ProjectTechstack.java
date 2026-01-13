package com.umc.devine.domain.techstack.entity.mapping;

import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.techstack.entity.Techstack;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "project_techstack")
public class ProjectTechstack {

    @EmbeddedId
    private ProjectTechstackId id;

    @MapsId("projectId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @MapsId("techstackId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teckstack_id")
    private Techstack techstack;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ProjectTechstackId implements Serializable {
        private Long projectId;
        private Long techstackId;
    }
}
