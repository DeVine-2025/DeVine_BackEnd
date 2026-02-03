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

        String gitUrl = gitRepoUrl.getGitUrl();
        String clerkUserId = gitRepoUrl.getMember().getClerkId();

        // COMBINED 방식: 이벤트 1개만 발행
        eventPublisher.publishEvent(ReportCreatedEvent.builder()
                .mainReportId(savedMainReport.getId())
                .detailReportId(savedDetailReport.getId())
                .gitUrl(gitUrl)
                .clerkId(clerkUserId)
                .build());

        log.info("Report 생성 요청 - memberId: {}, gitRepoId: {}, mainReportId: {}, detailReportId: {}",
                memberId, request.gitRepoId(), savedMainReport.getId(), savedDetailReport.getId());

        return ReportConverter.toCreateReportRes(savedMainReport, savedDetailReport);
    }


    @Override
    public void processCallback(ReportReqDTO.CallbackReq request) {
        DevReport mainReport = devReportRepository.findById(request.mainReportId())
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));
        DevReport detailReport = devReportRepository.findById(request.detailReportId())
                .orElseThrow(() -> new ReportException(ReportErrorCode.REPORT_NOT_FOUND));

        switch (request.status()) {
            case SUCCESS -> {
                if (request.content() == null || request.content().isNull()) {
                    log.warn("리포트 content가 비어있음 - mainReportId: {}, detailReportId: {}",
                            request.mainReportId(), request.detailReportId());
                    throw new ReportException(ReportErrorCode.INVALID_JSON_FORMAT);
                }

                var mainContent = request.content().get("main");
                var detailContent = request.content().get("detail");

                if (mainContent == null || mainContent.isNull()) {
                    log.warn("메인 리포트 content가 비어있음 - mainReportId: {}", request.mainReportId());
                    throw new ReportException(ReportErrorCode.INVALID_JSON_FORMAT);
                }
                if (detailContent == null || detailContent.isNull()) {
                    log.warn("상세 리포트 content가 비어있음 - detailReportId: {}", request.detailReportId());
                    throw new ReportException(ReportErrorCode.INVALID_JSON_FORMAT);
                }

                mainReport.completeReport(mainContent.toString());
                detailReport.completeReport(detailContent.toString());
                log.info("리포트 생성 완료 - mainReportId: {}, detailReportId: {}", request.mainReportId(), request.detailReportId());
            }
            case FAILED -> {
                mainReport.failReport(request.errorMessage());
                detailReport.failReport(request.errorMessage());
                log.warn("리포트 생성 실패 - mainReportId: {}, detailReportId: {}, error: {}",
                        request.mainReportId(), request.detailReportId(), request.errorMessage());
            }
        }
    }

    @Override
    public void deleteReport(Long reportId) {
        devReportRepository.deleteById(reportId);
        log.info("리포트 삭제 완료 - reportId: {}", reportId);
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
