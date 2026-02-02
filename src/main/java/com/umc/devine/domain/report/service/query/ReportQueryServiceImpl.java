package com.umc.devine.domain.report.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.report.converter.ReportConverter;
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
    private final ObjectMapper objectMapper;

    @Override
    public ReportResDTO.ReportRes getMainReport(Long memberId, Long gitRepoId) {
        DevReport report = reportRepository.findByGitRepoIdAndReportTypeWithMember(gitRepoId, ReportType.MAIN)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        validateVisibility(report, memberId);

        JsonNode contentJson = parseJsonContent(report.getContent(), report.getId());
        return ReportConverter.toReportRes(report, contentJson);
    }

    @Override
    public ReportResDTO.ReportRes getDetailReport(Long memberId, Long gitRepoId) {
        DevReport report = reportRepository.findByGitRepoIdAndReportTypeWithMember(gitRepoId, ReportType.DETAIL)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        validateVisibility(report, memberId);

        JsonNode contentJson = parseJsonContent(report.getContent(), report.getId());
        return ReportConverter.toReportRes(report, contentJson);
    }

    private void validateVisibility(DevReport report, Long memberId) {
        if (report.getVisibility() == ReportVisibility.PRIVATE) {
            Long ownerId = report.getGitRepoUrl().getMember().getId();
            if (!ownerId.equals(memberId)) {
                throw new ReportException(ReportErrorCode.UNAUTHORIZED_ACCESS);
            }
        }
    }

    private JsonNode parseJsonContent(String content, Long reportId) {
        if (content == null || content.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException e) {
            log.error("리포트 JSON 파싱 실패 - reportId: {}, content: {}",
                    reportId, content.substring(0, Math.min(100, content.length())), e);
            return null;
        }
    }
}