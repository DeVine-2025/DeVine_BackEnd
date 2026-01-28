package com.umc.devine.domain.report.service.query;

import com.umc.devine.domain.report.converter.ReportConverter;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.enums.ReportVisibility;
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
@Transactional(readOnly = true)
public class ReportQueryServiceImpl implements ReportQueryService {

    private final DevReportRepository reportRepository;

    @Override
    public ReportResDTO.ReportRes getMainReport(Long memberId, Long gitRepoId) {
        DevReport report = reportRepository.findByGitRepoIdAndReportTypeWithMember(gitRepoId, ReportType.MAIN)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        validateVisibility(report, memberId);

        return ReportConverter.toReportRes(report);
    }

    @Override
    public ReportResDTO.ReportRes getDetailReport(Long memberId, Long gitRepoId) {
        DevReport report = reportRepository.findByGitRepoIdAndReportTypeWithMember(gitRepoId, ReportType.DETAIL)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        validateVisibility(report, memberId);

        return ReportConverter.toReportRes(report);
    }

    private void validateVisibility(DevReport report, Long memberId) {
        if (report.getVisibility() == ReportVisibility.PRIVATE) {
            Long ownerId = report.getGitRepoUrl().getMember().getId();
            if (!ownerId.equals(memberId)) {
                throw new ReportException(ReportErrorCode.UNAUTHORIZED_ACCESS);
            }
        }
    }
}