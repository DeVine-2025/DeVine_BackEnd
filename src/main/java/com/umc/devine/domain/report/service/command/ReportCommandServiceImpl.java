package com.umc.devine.domain.report.service.command;

import com.umc.devine.domain.report.converter.ReportConverter;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorCode;
import com.umc.devine.domain.report.repository.DevReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandServiceImpl implements ReportCommandService {

    private final DevReportRepository devReportRepository;

    @Override
    public ReportResDTO.UpdateVisibilityRes updateVisibility(Long memberId, Long reportId, ReportReqDTO.UpdateVisibilityReq request) {
        DevReport report = devReportRepository.findByIdWithMember(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        validateOwnership(report, memberId);

        report.updateVisibility(request.visibility());

        log.info("Report 공개 범위 수정 memberId: {}, reportId: {}, visibility: {}", memberId, reportId, request.visibility());

        return ReportConverter.toUpdateVisibilityRes(report);
    }

    private void validateOwnership(DevReport report, Long memberId) {
        Long ownerId = report.getGitRepoUrl().getMember().getId();
        if (!ownerId.equals(memberId)) {
            log.warn("리포트 권한 없음 - memberId: {}, reportId: {}, ownerId: {}", memberId, report.getId(), ownerId);
            throw new ReportException(ReportErrorCode.UNAUTHORIZED_ACCESS);
        }
    }
}
