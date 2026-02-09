package com.umc.devine.domain.techstack.entity.mapping;

import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.techstack.entity.Techstack;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "report_techstack",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dev_report_id", "techstack_id"}))
public class ReportTechstack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dev_report_id", nullable = false)
    private DevReport devReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "techstack_id", nullable = false)
    private Techstack techstack;

    @Column(name = "report_techstack_rate", nullable = false)
    private Integer rate;
}
