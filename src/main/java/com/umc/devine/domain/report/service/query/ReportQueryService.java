package com.umc.devine.domain.report.service.query;

import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;

public interface ReportQueryService {

    ReportResDTO.ReportRes getMainReport(Long memberId, Long gitRepoId);

    ReportResDTO.ReportRes getDetailReport(Long memberId, Long gitRepoId);
}

