package com.umc.devine.domain.report.service.query;

import com.umc.devine.domain.report.converter.ReportConverter;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorCode;
import com.umc.devine.domain.report.repository.DevReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryServiceImpl implements ReportQueryService {

    private final DevReportRepository reportRepository;

    @Override
    public ReportResDTO.ReportRes getMainReport(Long gitRepoId) {
        DevReport report = reportRepository.findByGitRepoIdAndReportType(gitRepoId, ReportType.MAIN)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        return ReportConverter.toReportRes(report);
    }

    @Override
    public ReportResDTO.ReportRes getDetailReport(Long gitRepoId) {
        DevReport report = reportRepository.findByGitRepoIdAndReportType(gitRepoId, ReportType.DETAIL)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        return ReportConverter.toReportRes(report);
    }
}