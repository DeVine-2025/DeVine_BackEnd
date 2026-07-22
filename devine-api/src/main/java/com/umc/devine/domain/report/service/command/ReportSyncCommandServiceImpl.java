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
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.exception.code.ReportErrorReason;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.techstack.service.command.DevTechstackCommandService;
import com.umc.devine.domain.ticket.service.command.ReportCreditCommandService;
import com.umc.devine.infrastructure.fastapi.FastApiSyncReportClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * 동기 리포트 생성 전용 서비스. 프론트 비동기 전환 시 이 클래스를 파일째 제거하면 된다.
 */
@Deprecated // TODO : 프론트 비동기 전환 후 제거 예정
@RequiredArgsConstructor
@Slf4j
@Service
public class ReportSyncCommandServiceImpl implements ReportSyncCommandService {

    private final DevReportRepository devReportRepository;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final MemberRepository memberRepository;
    private final DevTechstackCommandService devTechstackCommandService;
    private final ReportCreditCommandService reportCreditCommandService;
    private final FastApiSyncReportClient fastApiSyncReportClient;
    // 메서드 자체는 트랜잭션 밖에서 돌기 때문에 각 구간을 직접 트랜잭션으로 감싼다.
    private final TransactionTemplate txTemplate;

    @Override
    public ReportResDTO.CreateReportSyncRes createReportSync(Long memberId, ReportReqDTO.CreateReportReq request) {
        PreparedReports prepared = txTemplate.execute(status -> prepareReports(memberId, request));

        log.info("Report 동기 생성 시작 - memberId: {}, gitRepoId: {}, mainReportId: {}, detailReportId: {}",
                memberId, request.gitRepoId(), prepared.mainReportId(), prepared.detailReportId());

        try {
            FastApiResDto.ReportGenerationSyncRes response = fastApiSyncReportClient.requestReportGenerationSync(
                    prepared.mainReportId(), prepared.detailReportId(), prepared.gitUrl(), prepared.clerkId());

            ReportContents contents = extractContents(response);

            ReportResDTO.CreateReportSyncRes result =
                    txTemplate.execute(status -> completeReports(prepared, contents, response.techstacks()));

            log.info("Report 동기 생성 완료 - mainReportId: {}, detailReportId: {}",
                    prepared.mainReportId(), prepared.detailReportId());

            return result;
        } catch (Exception e) {
            log.error("Report 동기 생성 실패 - mainReportId: {}, detailReportId: {}",
                    prepared.mainReportId(), prepared.detailReportId(), e);

            // 취소 처리가 실패해도 원래 실패 원인을 가리지 않도록 여기서 삼킨다.
            try {
                txTemplate.executeWithoutResult(status -> cancelReports(prepared));
            } catch (Exception cancelFailure) {
                log.error("Report 동기 생성 취소 실패 — 수동 복구 필요, mainReportId: {}, detailReportId: {}",
                        prepared.mainReportId(), prepared.detailReportId(), cancelFailure);
            }

            if (e instanceof ReportException re) throw re;
            throw new ReportException(ReportErrorReason.REPORT_GENERATION_FAILED);
        }
    }

    // 검증 → 크레딧 차감 → 리포트 저장. 하나라도 실패하면 전부 롤백되므로 별도 보상 처리가 필요 없다.
    private PreparedReports prepareReports(Long memberId, ReportReqDTO.CreateReportReq request) {
        GitRepoUrl gitRepoUrl = gitRepoUrlRepository.findByIdWithMember(request.gitRepoId())
                .orElseThrow(() -> new ReportException(ReportErrorReason.GIT_REPO_NOT_FOUND));

        validateGitRepoOwnership(gitRepoUrl, memberId);
        validateReportNotExists(request.gitRepoId());

        Member member = gitRepoUrl.getMember();
        reportCreditCommandService.useCreditAtomic(member);

        DevReport mainReport = saveReportWithDuplicateCheck(ReportConverter.toReport(gitRepoUrl, ReportType.MAIN));
        DevReport detailReport = saveReportWithDuplicateCheck(ReportConverter.toReport(gitRepoUrl, ReportType.DETAIL));

        return new PreparedReports(mainReport.getId(), detailReport.getId(),
                member.getId(), gitRepoUrl.getGitUrl(), member.getClerkId());
    }

    private ReportContents extractContents(FastApiResDto.ReportGenerationSyncRes response) {
        if (response == null || !"SUCCESS".equals(response.status())) {
            log.warn("FastAPI 동기 응답 실패 - error: {}", response != null ? response.errorMessage() : "응답이 없습니다.");
            throw new ReportException(ReportErrorReason.REPORT_GENERATION_FAILED);
        }

        JsonNode content = response.content();
        JsonNode mainContent = content == null ? null : content.get("main");
        JsonNode detailContent = content == null ? null : content.get("detail");

        if (mainContent == null || mainContent.isNull() || detailContent == null || detailContent.isNull()) {
            log.warn("FastAPI 동기 응답 content가 비어있음 - mainReportId: {}, detailReportId: {}",
                    response.mainReportId(), response.detailReportId());
            throw new ReportException(ReportErrorReason.INVALID_JSON_FORMAT);
        }

        return new ReportContents(mainContent, detailContent);
    }

    private ReportResDTO.CreateReportSyncRes completeReports(
            PreparedReports prepared, ReportContents contents, List<String> techstacks) {
        DevReport mainReport = devReportRepository.findByIdWithMember(prepared.mainReportId())
                .orElseThrow(() -> new ReportException(ReportErrorReason.REPORT_NOT_FOUND));
        DevReport detailReport = devReportRepository.findById(prepared.detailReportId())
                .orElseThrow(() -> new ReportException(ReportErrorReason.REPORT_NOT_FOUND));

        mainReport.completeReport(contents.main().toString());
        detailReport.completeReport(contents.detail().toString());

        devTechstackCommandService.saveAutoTechstacks(mainReport.getGitRepoUrl().getMember(), techstacks);

        return ReportConverter.toCreateReportSyncRes(mainReport, detailReport, contents.main(), contents.detail());
    }

    // 행 삭제와 크레딧 환불을 한 트랜잭션으로 묶어 둘 중 하나만 반영되는 상황을 막는다.
    private void cancelReports(PreparedReports prepared) {
        // 진행 중인 리포트만 삭제한다. 이미 완료/실패로 종결된 경우 0건 삭제되고, 이때는 환불하지 않는다 (이중 환불 방지).
        int deleted = devReportRepository.deleteInProgressByIdIn(List.of(prepared.mainReportId(), prepared.detailReportId()));
        if (deleted == 0) {
            log.warn("이미 종결된 리포트 - 삭제·환불 생략, mainReportId: {}, detailReportId: {}",
                    prepared.mainReportId(), prepared.detailReportId());
            return;
        }

        reportCreditCommandService.refundCreditInCurrentTransaction(memberRepository.getReferenceById(prepared.memberId()));
    }

    private void validateGitRepoOwnership(GitRepoUrl gitRepoUrl, Long memberId) {
        Long ownerId = gitRepoUrl.getMember().getId();
        if (!ownerId.equals(memberId)) {
            log.warn("Git 저장소 권한 없음 - memberId: {}, gitRepoId: {}, ownerId: {}", memberId, gitRepoUrl.getId(), ownerId);
            throw new ReportException(ReportErrorReason.UNAUTHORIZED_ACCESS);
        }
    }

    // 실패한 리포트는 partial unique index에서 제외되어 재시도 가능
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

    private record PreparedReports(Long mainReportId, Long detailReportId, Long memberId, String gitUrl, String clerkId) {}

    private record ReportContents(JsonNode main, JsonNode detail) {}
}
