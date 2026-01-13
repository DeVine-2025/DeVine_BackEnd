package com.umc.devine.domain.techstack.entity.mapping;

import com.umc.devine.domain.techstack.entity.DevReport;
import com.umc.devine.domain.techstack.entity.Techstack;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "report_techstack")
public class ReportTechstack {

    @EmbeddedId
    private ReportTechstackId id;

    @MapsId("reportId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dev_report_id")
    private DevReport devReport;

    @MapsId("techstackId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teckstack_id")
    private Techstack techstack;

    @Column(name = "report_teckstack_rate", nullable = false)
    private Integer rate;

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ReportTechstackId implements Serializable {
        private Long reportId;
        private Long techstackId;
    }
}
