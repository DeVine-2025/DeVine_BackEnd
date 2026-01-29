package com.umc.devine.domain.report.service.command;

import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;

public interface ReportCommandService {
    ReportResDTO.UpdateVisibilityRes updateVisibility(Long memberId, Long reportId, ReportReqDTO.UpdateVisibilityReq request);
    ReportResDTO.CreateReportRes createReport(Long memberId, ReportReqDTO.CreateReportReq request);
    void processCallback(ReportReqDTO.CallbackReq request);
}
