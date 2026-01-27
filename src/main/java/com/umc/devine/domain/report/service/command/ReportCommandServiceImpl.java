package com.umc.devine.domain.report.service.command;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.report.converter.ReportConverter;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.event.ReportCreatedEvent;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorCode;
import com.umc.devine.domain.report.repository.DevReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandServiceImpl implements ReportCommandService {

    private final DevReportRepository devReportRepository;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ReportResDTO.UpdateVisibilityRes updateVisibility(Long memberId, Long reportId, ReportReqDTO.UpdateVisibilityReq request) {
        DevReport report = devReportRepository.findByIdWithMember(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        validateOwnership(report, memberId);

        report.updateVisibility(request.visibility());

        log.info("Report 공개 범위 수정 memberId: {}, reportId: {}, visibility: {}", memberId, reportId, request.visibility());

        return ReportConverter.toUpdateVisibilityRes(report);
    }

    @Override
    public ReportResDTO.CreateReportRes createReport(Long memberId, ReportReqDTO.CreateReportReq request) {
        GitRepoUrl gitRepoUrl = gitRepoUrlRepository.findByIdWithMember(request.gitRepoId())
                .orElseThrow(() -> new ReportException(ReportErrorCode.GIT_REPO_NOT_FOUND));

        validateGitRepoOwnership(gitRepoUrl, memberId);
        validateReportNotExists(request.gitRepoId());

        DevReport mainReport = ReportConverter.toReport(gitRepoUrl, ReportType.MAIN);
        DevReport detailReport = ReportConverter.toReport(gitRepoUrl, ReportType.DETAIL);

        DevReport savedMainReport = devReportRepository.save(mainReport);
        DevReport savedDetailReport = devReportRepository.save(detailReport);

        log.info("Report 생성 요청 - memberId: {}, gitRepoId: {}, mainReportId: {}, detailReportId: {}",
                memberId, request.gitRepoId(), savedMainReport.getId(), savedDetailReport.getId());

        return ReportConverter.toCreateReportRes(savedMainReport, savedDetailReport);
    }

    private void validateOwnership(DevReport report, Long memberId) {
        Long ownerId = report.getGitRepoUrl().getMember().getId();
        if (!ownerId.equals(memberId)) {
            log.warn("리포트 권한 없음 - memberId: {}, reportId: {}, ownerId: {}", memberId, report.getId(), ownerId);
            throw new ReportException(ReportErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void validateGitRepoOwnership(GitRepoUrl gitRepoUrl, Long memberId) {
        Long ownerId = gitRepoUrl.getMember().getId();
        if (!ownerId.equals(memberId)) {
            log.warn("Git 저장소 권한 없음 - memberId: {}, gitRepoId: {}, ownerId: {}", memberId, gitRepoUrl.getId(), ownerId);
            throw new ReportException(ReportErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    private void validateReportNotExists(Long gitRepoId) {
        if (devReportRepository.existsByGitRepoUrlId(gitRepoId)) {
            log.warn("리포트 중복 생성 시도 - gitRepoId: {}", gitRepoId);
            throw new ReportException(ReportErrorCode.REPORT_ALREADY_EXISTS);
        }
    }
}
