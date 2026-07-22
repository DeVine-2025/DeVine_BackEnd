package com.umc.devine.domain.report.service.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.report.converter.ReportConverter;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.event.ReportCreatedEvent;
import com.umc.devine.domain.report.event.ReportNotificationEvent;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorReason;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.techstack.service.command.DevTechstackCommandService;
import com.umc.devine.domain.ticket.service.command.ReportCreditCommandService;
import com.umc.devine.infrastructure.fastapi.FastApiSyncReportClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandServiceImpl implements ReportCommandService {

    private final DevReportRepository devReportRepository;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final FastApiSyncReportClient fastApiSyncReportClient;
    private final PlatformTransactionManager transactionManager;
    private final DevTechstackCommandService devTechstackCommandService;
    private final ReportCreditCommandService reportCreditCommandService;
    private TransactionTemplate requiresNewTxTemplate;

    @PostConstruct
    void initTransactionTemplate() {
        this.requiresNewTxTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public ReportResDTO.UpdateVisibilityRes updateVisibility(Long memberId, Long reportId, ReportReqDTO.UpdateVisibilityReq request) {
        DevReport report = devReportRepository.findByIdWithMember(reportId)
                .orElseThrow(() -> new ReportException(ReportErrorReason.REPORT_NOT_FOUND));

        validateOwnership(report, memberId);

        report.updateVisibility(request.visibility());

        log.info("Report 공개 범위 수정 memberId: {}, reportId: {}, visibility: {}", memberId, reportId, request.visibility());

        return ReportConverter.toUpdateVisibilityRes(report);
    }

    @Override
    public ReportResDTO.CreateReportRes createReport(Long memberId, ReportReqDTO.CreateReportReq request) {
        GitRepoUrl gitRepoUrl = gitRepoUrlRepository.findByIdWithMember(request.gitRepoId())
                .orElseThrow(() -> new ReportException(ReportErrorReason.GIT_REPO_NOT_FOUND));

        validateGitRepoOwnership(gitRepoUrl, memberId);
        validateReportNotExists(request.gitRepoId());

        Member member = gitRepoUrl.getMember();
        reportCreditCommandService.useCreditAtomic(member);

        DevReport savedMainReport = saveReportWithDuplicateCheck(ReportConverter.toReport(gitRepoUrl, ReportType.MAIN));
        DevReport savedDetailReport = saveReportWithDuplicateCheck(ReportConverter.toReport(gitRepoUrl, ReportType.DETAIL));

        eventPublisher.publishEvent(ReportCreatedEvent.builder()
                .mainReportId(savedMainReport.getId())
                .detailReportId(savedDetailReport.getId())
                .gitUrl(gitRepoUrl.getGitUrl())
                .clerkId(gitRepoUrl.getMember().getClerkId())
                .memberId(memberId)
                .build());

        log.info("Report 생성 요청 - memberId: {}, gitRepoId: {}, mainReportId: {}, detailReportId: {}",
                memberId, request.gitRepoId(), savedMainReport.getId(), savedDetailReport.getId());

        return ReportConverter.toCreateReportRes(savedMainReport, savedDetailReport);
    }

    @Deprecated // TODO : 비동기 전환 후 제거 예정
    @Override
    @Transactional(noRollbackFor = ReportException.class)
    public ReportResDTO.CreateReportSyncRes createReportSync(Long memberId, ReportReqDTO.CreateReportReq request) {
        // 1. Git 저장소 조회 및 권한 검증
        GitRepoUrl gitRepoUrl = gitRepoUrlRepository.findByIdWithMember(request.gitRepoId())
                .orElseThrow(() -> new ReportException(ReportErrorReason.GIT_REPO_NOT_FOUND));

        validateGitRepoOwnership(gitRepoUrl, memberId);

        // 2. 활성 리포트 중복 체크 (실패한 리포트는 partial unique index에서 제외되어 재시도 가능)
        validateReportNotExists(request.gitRepoId());

        Member member = gitRepoUrl.getMember();
        requiresNewTxTemplate.executeWithoutResult(status ->
                reportCreditCommandService.useCreditAtomic(member));

        // 3. MAIN/DETAIL 리포트 엔티티 생성·저장 및 FastAPI 호출
        DevReport savedMainReport = null;
        DevReport savedDetailReport = null;

        try {
            // saveAndFlush로 즉시 INSERT하여 리포트 ID 확정 및 중복 체크 (별도 트랜잭션으로 트랜잭션 오염 방지)
            savedMainReport = saveReportWithDuplicateCheckInNewTransaction(ReportConverter.toReport(gitRepoUrl, ReportType.MAIN));
            savedDetailReport = saveReportWithDuplicateCheckInNewTransaction(ReportConverter.toReport(gitRepoUrl, ReportType.DETAIL));

            log.info("Report 동기 생성 시작 - memberId: {}, gitRepoId: {}, mainReportId: {}, detailReportId: {}",
                    memberId, request.gitRepoId(), savedMainReport.getId(), savedDetailReport.getId());

            // 4. FastAPI 동기 호출 및 결과 처리
            FastApiResDto.ReportGenerationSyncRes response = fastApiSyncReportClient.requestReportGenerationSync(
                    savedMainReport, savedDetailReport, gitRepoUrl.getGitUrl(), member.getClerkId());

            // 4-1. 응답 상태 검증
            if (response == null || !"SUCCESS".equals(response.status())) {
                String errorMessage = response != null ? response.errorMessage() : "응답이 없습니다.";
                log.warn("Report 동기 생성 실패 - mainReportId: {}, detailReportId: {}, error: {}",
                        savedMainReport.getId(), savedDetailReport.getId(), errorMessage);
                // 실패 시 catch 블록에서 리포트 행을 삭제하고 크레딧을 환불한다.
                throw new ReportException(ReportErrorReason.REPORT_GENERATION_FAILED);
            }

            // 4-2. content 존재 여부 검증
            JsonNode content = response.content();
            if (content == null || content.isNull()) {
                log.warn("Report content가 null - mainReportId: {}, detailReportId: {}",
                        savedMainReport.getId(), savedDetailReport.getId());
                throw new ReportException(ReportErrorReason.INVALID_JSON_FORMAT);
            }

            // 4-3. main/detail 필드 검증
            JsonNode mainContent = content.get("main");
            JsonNode detailContent = content.get("detail");

            if (mainContent == null || mainContent.isNull() || detailContent == null || detailContent.isNull()) {
                log.warn("Report content가 비어있음 - mainReportId: {}, detailReportId: {}",
                        savedMainReport.getId(), savedDetailReport.getId());
                throw new ReportException(ReportErrorReason.INVALID_JSON_FORMAT);
            }

            // 5. 리포트 완료 처리
            savedMainReport.completeReport(mainContent.toString());
            savedDetailReport.completeReport(detailContent.toString());

            // 6. 응답에서 techstacks 추출하여 DevTechstack AUTO로 저장
            devTechstackCommandService.saveAutoTechstacks(member, response.techstacks());

            log.info("Report 동기 생성 완료 - mainReportId: {}, detailReportId: {}",
                    savedMainReport.getId(), savedDetailReport.getId());

            return ReportConverter.toCreateReportSyncRes(savedMainReport, savedDetailReport, mainContent, detailContent);

        } catch (Exception e) {
            log.error("Report 동기 생성 중 예외 발생 - mainReportId: {}, detailReportId: {}",
                    savedMainReport != null ? savedMainReport.getId() : null,
                    savedDetailReport != null ? savedDetailReport.getId() : null, e);

            // [동기 실패 경로] 리포트 행을 삭제하고 크레딧을 환불한다.
            // 비동기 콜백 실패(handleCallbackFailed, handleCallbackSuccess catch)와 달리,
            // 동기 경로는 FAILED 상태를 남기지 않고 행 자체를 제거한다. (재시도를 허용하기 위해)
            if (savedMainReport != null) {
                entityManager.detach(savedMainReport);
                devReportRepository.deleteById(savedMainReport.getId());
            }
            if (savedDetailReport != null) {
                entityManager.detach(savedDetailReport);
                devReportRepository.deleteById(savedDetailReport.getId());
            }
            reportCreditCommandService.refundCredit(member);

            if (e instanceof ReportException re) throw re;

            throw new ReportException(ReportErrorReason.REPORT_GENERATION_FAILED);
        }
    }

    @Override
    @Transactional(noRollbackFor = ReportException.class)
    public void processCallback(ReportReqDTO.CallbackReq request) {
        DevReport mainReport = devReportRepository.findByIdWithMemberForUpdate(request.mainReportId())
                .orElseThrow(() -> new ReportException(ReportErrorReason.REPORT_NOT_FOUND));
        DevReport detailReport = devReportRepository.findByIdWithMemberForUpdate(request.detailReportId())
                .orElseThrow(() -> new ReportException(ReportErrorReason.REPORT_NOT_FOUND));

        validateCallbackReportPair(mainReport, detailReport);

        if (mainReport.getCompletedAt() != null || mainReport.getErrorMessage() != null) {
            log.warn("이미 처리된 리포트에 콜백 재호출 무시 - mainReportId: {}, detailReportId: {}",
                    request.mainReportId(), request.detailReportId());
            return;
        }

        switch (request.status()) {
            case SUCCESS -> handleCallbackSuccess(mainReport, detailReport, request);
            case FAILED -> handleCallbackFailed(mainReport, detailReport, request);
        }
    }

    private void handleCallbackSuccess(DevReport mainReport, DevReport detailReport, ReportReqDTO.CallbackReq request) {
        Member member = mainReport.getGitRepoUrl().getMember();
        try {
            if (request.content() == null || request.content().isNull()) {
                log.warn("리포트 content가 비어있음 - mainReportId: {}, detailReportId: {}",
                        request.mainReportId(), request.detailReportId());
                throw new ReportException(ReportErrorReason.INVALID_JSON_FORMAT);
            }

            var mainContent = request.content().get("main");
            var detailContent = request.content().get("detail");

            if (mainContent == null || mainContent.isNull()) {
                log.warn("메인 리포트 content가 비어있음 - mainReportId: {}", request.mainReportId());
                throw new ReportException(ReportErrorReason.INVALID_JSON_FORMAT);
            }
            if (detailContent == null || detailContent.isNull()) {
                log.warn("상세 리포트 content가 비어있음 - detailReportId: {}", request.detailReportId());
                throw new ReportException(ReportErrorReason.INVALID_JSON_FORMAT);
            }

            mainReport.completeReport(mainContent.toString());
            detailReport.completeReport(detailContent.toString());

            try {
                devTechstackCommandService.saveAutoTechstacks(member, request.techstacks());
            } catch (Exception e) {
                log.error("기술스택 저장 실패 (리포트는 정상 완료) - mainReportId: {}, memberId: {}",
                        mainReport.getId(), member.getId(), e);
            }

            log.info("리포트 생성 완료 - mainReportId: {}, detailReportId: {}", request.mainReportId(), request.detailReportId());

            publishReportNotificationEvent(member.getId(), mainReport.getId(), mainReport.getGitRepoUrl().getGitUrl(), true);
        } catch (Exception e) {
            mainReport.failReport(e.getMessage());
            detailReport.failReport(e.getMessage());
            reportCreditCommandService.refundCreditInCurrentTransaction(member);
            publishReportNotificationEvent(member.getId(), mainReport.getId(), mainReport.getGitRepoUrl().getGitUrl(), false);

            if (e instanceof ReportException re) throw re;
            throw new ReportException(ReportErrorReason.REPORT_GENERATION_FAILED);
        }
    }

    private void handleCallbackFailed(DevReport mainReport, DevReport detailReport, ReportReqDTO.CallbackReq request) {
        mainReport.failReport(request.errorMessage());
        detailReport.failReport(request.errorMessage());
        log.warn("리포트 생성 실패 - mainReportId: {}, detailReportId: {}, error: {}",
                request.mainReportId(), request.detailReportId(), request.errorMessage());

        Member member = mainReport.getGitRepoUrl().getMember();
        reportCreditCommandService.refundCreditInCurrentTransaction(member);
        publishReportNotificationEvent(member.getId(), mainReport.getId(), mainReport.getGitRepoUrl().getGitUrl(), false);
    }

    @Override
    public void handleReportDispatchFailed(Long mainReportId, Long detailReportId, Long memberId, String gitUrl, String reason) {
        log.error("리포트 생성 요청 전송 실패 처리 - mainReportId: {}, detailReportId: {}, memberId: {}, reason: {}",
                mainReportId, detailReportId, memberId, reason);

        // 진행 중인 리포트만 삭제한다. 동시 콜백이 이미 완료/실패로 종결한 경우 0건 삭제되고,
        // 이때는 크레딧 환불·실패 알림을 수행하지 않는다 (리포트가 정상 처리되었을 수 있으므로).
        int deleted = devReportRepository.deleteInProgressByIdIn(List.of(mainReportId, detailReportId));
        if (deleted == 0) {
            log.warn("이미 완료/처리된 리포트 - 삭제·환불·알림 생략, mainReportId: {}, detailReportId: {}",
                    mainReportId, detailReportId);
            return;
        }

        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            log.error("크레딧 환불 실패 - 회원 없음, memberId: {}, mainReportId: {}", memberId, mainReportId);
        } else {
            reportCreditCommandService.refundCredit(member);
        }

        publishReportNotificationEvent(memberId, mainReportId, gitUrl, false);
    }

    private void publishReportNotificationEvent(Long receiverId, Long reportId, String gitUrl, boolean success) {
        eventPublisher.publishEvent(ReportNotificationEvent.builder()
                .receiverId(receiverId)
                .reportId(reportId)
                .gitUrl(gitUrl)
                .success(success)
                .build());
    }

    private void validateOwnership(DevReport report, Long memberId) {
        Long ownerId = report.getGitRepoUrl().getMember().getId();
        if (!ownerId.equals(memberId)) {
            log.warn("리포트 권한 없음 - memberId: {}, reportId: {}, ownerId: {}", memberId, report.getId(), ownerId);
            throw new ReportException(ReportErrorReason.UNAUTHORIZED_ACCESS);
        }
    }

    private void validateGitRepoOwnership(GitRepoUrl gitRepoUrl, Long memberId) {
        Long ownerId = gitRepoUrl.getMember().getId();
        if (!ownerId.equals(memberId)) {
            log.warn("Git 저장소 권한 없음 - memberId: {}, gitRepoId: {}, ownerId: {}", memberId, gitRepoUrl.getId(), ownerId);
            throw new ReportException(ReportErrorReason.UNAUTHORIZED_ACCESS);
        }
    }

    private void validateCallbackReportPair(DevReport mainReport, DevReport detailReport) {
        if (mainReport.getReportType() != ReportType.MAIN
                || detailReport.getReportType() != ReportType.DETAIL
                || !Objects.equals(mainReport.getGitRepoUrl().getId(), detailReport.getGitRepoUrl().getId())) {
            log.warn("리포트 콜백 쌍 불일치 - mainReportId: {}, mainType: {}, detailReportId: {}, detailType: {}",
                    mainReport.getId(), mainReport.getReportType(), detailReport.getId(), detailReport.getReportType());
            throw new ReportException(ReportErrorReason.INVALID_REPORT_PAIR);
        }
    }

    private void validateReportNotExists(Long gitRepoId) {
        if (devReportRepository.existsActiveReportByGitRepoUrlId(gitRepoId)) {
            log.warn("리포트 중복 생성 시도 - gitRepoId: {}", gitRepoId);
            throw new ReportException(ReportErrorReason.REPORT_ALREADY_EXISTS);
        }
    }

    // 동시 요청으로 인한 중복 삽입 처리 (partial unique index 위반 대응)
    private DevReport saveReportWithDuplicateCheck(DevReport report) {
        try {
            return devReportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            log.warn("리포트 중복 저장 시도 (동시 요청) - gitRepoId: {}, reportType: {}",
                    report.getGitRepoUrl().getId(), report.getReportType());
            throw new ReportException(ReportErrorReason.REPORT_ALREADY_EXISTS);
        }
    }

    // REQUIRES_NEW 트랜잭션에서 리포트 저장 (트랜잭션 오염 방지) 후 현재 영속성 컨텍스트에 merge
    private DevReport saveReportWithDuplicateCheckInNewTransaction(DevReport report) {
        DevReport saved = requiresNewTxTemplate.execute(status -> {
            try {
                return devReportRepository.saveAndFlush(report);
            } catch (DataIntegrityViolationException e) {
                log.warn("리포트 중복 저장 시도 (동시 요청) - gitRepoId: {}, reportType: {}",
                        report.getGitRepoUrl().getId(), report.getReportType());
                throw new ReportException(ReportErrorReason.REPORT_ALREADY_EXISTS);
            }
        });
        return devReportRepository.save(saved);
    }

}
