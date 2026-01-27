package com.umc.devine.domain.report.converter;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.enums.ReportVisibility;

public class ReportConverter {

    private static final String REPORT_GENERATION_REQUESTED_MESSAGE = "리포트 생성 요청이 접수되었습니다.";

    public static DevReport toReport(GitRepoUrl gitRepoUrl, ReportType reportType) {
        return DevReport.builder()
                .gitRepoUrl(gitRepoUrl)
                .reportType(reportType)
                .visibility(ReportVisibility.PUBLIC)
                .build();
    }

    public static ReportResDTO.ReportRes toReportRes(DevReport report) {
        return ReportResDTO.ReportRes.builder()
                .reportId(report.getId())
                .gitRepoId(report.getGitRepoUrl().getId())
                .gitRepoUrl(report.getGitRepoUrl().getGitUrl())
                .reportType(report.getReportType())
                .visibility(report.getVisibility())
                .content(report.getContent())
                .errorMessage(report.getErrorMessage())
                .completedAt(report.getCompletedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    public static ReportResDTO.UpdateVisibilityRes toUpdateVisibilityRes(DevReport report) {
        return ReportResDTO.UpdateVisibilityRes.builder()
                .reportId(report.getId())
                .visibility(report.getVisibility())
                .build();
    }

    public static ReportResDTO.CreateReportRes toCreateReportRes(DevReport mainReport, DevReport detailReport) {
        return ReportResDTO.CreateReportRes.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .gitRepoId(mainReport.getGitRepoUrl().getId())
                .message(REPORT_GENERATION_REQUESTED_MESSAGE)
                .build();
    }
}
