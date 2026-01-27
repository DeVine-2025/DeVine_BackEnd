package com.umc.devine.domain.report.service.query;

import com.umc.devine.domain.report.dto.ReportResDTO;

public interface ReportQueryService {

    ReportResDTO.ReportRes getMainReport(Long gitRepoId);

    ReportResDTO.ReportRes getDetailReport(Long gitRepoId);
}

