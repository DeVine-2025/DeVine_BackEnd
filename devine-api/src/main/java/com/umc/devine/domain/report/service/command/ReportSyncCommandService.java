package com.umc.devine.domain.report.service.command;

import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;

/** 프론트 비동기 전환 전까지 유지하는 동기 리포트 생성. 전환 완료 시 이 인터페이스와 구현체를 통째로 제거한다. */
@Deprecated
public interface ReportSyncCommandService {
    ReportResDTO.CreateReportSyncRes createReportSync(Long memberId, ReportReqDTO.CreateReportReq request);
}
