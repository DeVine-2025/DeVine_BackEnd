package com.umc.devine.domain.report.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.global.dto.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface ReportQueryService {

    ReportResDTO.ReportRes getMainReport(Long memberId, Long gitRepoId);

    ReportResDTO.ReportRes getDetailReport(Long memberId, Long gitRepoId);

    ReportResDTO.ReportSummaryListDTO getMyReports(Member member, ReportType reportType);

    PagedResponse<ReportResDTO.ReportSummaryDTO> getReportsByNickname(String nickname, ReportType reportType, Pageable pageable);
}

