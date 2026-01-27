package com.umc.devine.domain.report.converter;

import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.entity.DevReport;

public class ReportConverter {

    public static ReportResDTO.ReportRes toReportRes(DevReport report) {
        return ReportResDTO.ReportRes.builder()
                .reportId(report.getId())
                .gitRepoId(report.getGitRepoUrl().getId())
                .gitRepoUrl(report.getGitRepoUrl().getGitUrl())
                .reportType(report.getReportType())
                .visibility(report.getVisibility())
                .content(report.getContent())
                .completedAt(report.getCompletedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
