package com.umc.devine.domain.techstack.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class DevReportResDTO {
    // TODO: 합쳐지면 양식 통합하기
    @Builder
    public record ReportDTO(
            Long reportId,
            String gitUrl,
            String content,
            List<ReportTechStackResDTO.ReportTechstackDTO> techstacks,
            LocalDateTime createdAt
    ){}
    // TODO: 합쳐지면 양식 통합하기
    @Builder
    public record ReportListDTO(
            List<ReportDTO> reports
    ){}
}
