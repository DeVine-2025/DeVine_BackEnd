package com.umc.devine.domain.techstack.converter;

import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.domain.techstack.dto.ReportTechStackResDTO;
import com.umc.devine.domain.techstack.entity.DevReport;
import com.umc.devine.domain.techstack.entity.mapping.ReportTechstack;

import java.util.List;
import java.util.stream.Collectors;

public class DevReportConverter {

    public static ReportTechStackResDTO.ReportTechstackDTO toReportTechstackDTO(ReportTechstack reportTechstack) {
        return ReportTechStackResDTO.ReportTechstackDTO.builder()
                .techstackName(reportTechstack.getTechstack().getName().toString())
                .techGenre(reportTechstack.getTechstack().getGenre().toString())
                .rate(reportTechstack.getRate())
                .build();
    }

    public static DevReportResDTO.ReportDTO toReportDTO(DevReport report, List<ReportTechstack> reportTechstacks) {
        List<ReportTechStackResDTO.ReportTechstackDTO> techstackDTOs = reportTechstacks.stream()
                .map(DevReportConverter::toReportTechstackDTO)
                .collect(Collectors.toList());

        return DevReportResDTO.ReportDTO.builder()
                .reportId(report.getId())
                .gitUrl(report.getGitRepoUrl().getGitUrl())
                .content(report.getContent())
                .techstacks(techstackDTOs)
                .createdAt(report.getCreatedAt())
                .build();
    }

    public static DevReportResDTO.ReportListDTO toReportListDTO(List<DevReportResDTO.ReportDTO> reports) {
        return DevReportResDTO.ReportListDTO.builder()
                .reports(reports)
                .build();
    }
}
