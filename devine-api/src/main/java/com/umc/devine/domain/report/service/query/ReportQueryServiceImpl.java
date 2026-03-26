package com.umc.devine.domain.report.service.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.exception.code.MemberErrorReason;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.report.converter.ReportConverter;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.enums.ReportVisibility;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorReason;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryServiceImpl implements ReportQueryService {

    private final DevReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ReportResDTO.ReportRes getMainReport(Long memberId, Long gitRepoId) {
        DevReport report = reportRepository.findByGitRepoIdAndReportTypeWithMember(gitRepoId, ReportType.MAIN)
                .orElseThrow(() -> new ReportException(ReportErrorReason.REPORT_NOT_FOUND));

        validateVisibility(report, memberId);

        JsonNode contentJson = parseJsonContent(report.getContent(), report.getId());
        return ReportConverter.toReportRes(report, contentJson);
    }

    @Override
    public ReportResDTO.ReportRes getDetailReport(Long memberId, Long gitRepoId) {
        DevReport report = reportRepository.findByGitRepoIdAndReportTypeWithMember(gitRepoId, ReportType.DETAIL)
                .orElseThrow(() -> new ReportException(ReportErrorReason.REPORT_NOT_FOUND));

        validateVisibility(report, memberId);

        JsonNode contentJson = parseJsonContent(report.getContent(), report.getId());
        return ReportConverter.toReportRes(report, contentJson);
    }

    private void validateVisibility(DevReport report, Long memberId) {
        if (report.getVisibility() == ReportVisibility.PRIVATE) {
            Long ownerId = report.getGitRepoUrl().getMember().getId();
            if (!ownerId.equals(memberId)) {
                throw new ReportException(ReportErrorReason.UNAUTHORIZED_ACCESS);
            }
        }
    }

    @Override
    public ReportResDTO.ReportSummaryListDTO getMyReports(Member member, ReportType reportType) {
        List<DevReport> reports = reportRepository.findAllByMemberAndReportType(member, reportType);
        return ReportConverter.toReportSummaryListDTO(reports);
    }

    @Override
    public PagedResponse<ReportResDTO.ReportSummaryDTO> getReportsByNickname(String nickname, ReportType reportType, Pageable pageable) {
        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MemberException(MemberErrorReason.NOT_FOUND));

        if (!member.getDisclosure()) {
            throw new MemberException(MemberErrorReason.PROFILE_NOT_PUBLIC);
        }

        Page<DevReport> reportPage = reportRepository.findAllByMemberAndReportTypeAndVisibility(
                member, reportType, ReportVisibility.PUBLIC, pageable);

        return PagedResponse.of(reportPage, reportPage.getContent().stream()
                .map(ReportConverter::toReportSummaryDTO)
                .toList());
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