package com.umc.devine.domain.report.service.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
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
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.infrastructure.fastapi.FastApiSyncReportClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandServiceImpl implements ReportCommandService {

    private final DevReportRepository devReportRepository;
    private final GitRepoUrlRepository gitRepoUrlRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FastApiSyncReportClient fastApiSyncReportClient;
    private final PlatformTransactionManager transactionManager;
    private final TechstackRepository techstackRepository;
    private final DevTechstackRepository devTechstackRepository;
    private TransactionTemplate requiresNewTxTemplate;

    @PostConstruct
    void initTransactionTemplate() {
        this.requiresNewTxTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

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

        DevReport savedMainReport = saveReportWithDuplicateCheck(mainReport);
        DevReport savedDetailReport = saveReportWithDuplicateCheck(detailReport);

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
    @Transactional(noRollbackFor = ReportException.class)
    public ReportResDTO.CreateReportSyncRes createReportSync(Long memberId, ReportReqDTO.CreateReportReq request) {
        // 1. Git 저장소 조회 및 권한 검증
        GitRepoUrl gitRepoUrl = gitRepoUrlRepository.findByIdWithMember(request.gitRepoId())
                .orElseThrow(() -> new ReportException(ReportErrorCode.GIT_REPO_NOT_FOUND));

        validateGitRepoOwnership(gitRepoUrl, memberId);

        // 2. 활성 리포트 중복 체크 (실패한 리포트는 partial unique index에서 제외되어 재시도 가능)
        validateReportNotExists(request.gitRepoId());

        // 3. MAIN/DETAIL 리포트 엔티티 생성 및 저장
        DevReport mainReport = ReportConverter.toReport(gitRepoUrl, ReportType.MAIN);
        DevReport detailReport = ReportConverter.toReport(gitRepoUrl, ReportType.DETAIL);

        // saveAndFlush로 즉시 INSERT하여 리포트 ID 확정 및 중복 체크 (별도 트랜잭션으로 트랜잭션 오염 방지)
        DevReport savedMainReport = saveReportWithDuplicateCheckInNewTransaction(mainReport);
        DevReport savedDetailReport = saveReportWithDuplicateCheckInNewTransaction(detailReport);

        String gitUrl = gitRepoUrl.getGitUrl();
        String clerkUserId = gitRepoUrl.getMember().getClerkId();

        log.info("Report 동기 생성 시작 - memberId: {}, gitRepoId: {}, mainReportId: {}, detailReportId: {}",
                memberId, request.gitRepoId(), savedMainReport.getId(), savedDetailReport.getId());

        // 4. FastAPI 동기 호출 및 결과 처리
        try {
            FastApiResDto.ReportGenerationSyncRes response = fastApiSyncReportClient.requestReportGenerationSync(
                    savedMainReport, savedDetailReport, gitUrl, clerkUserId);

            // 4-1. 응답 상태 검증
            if (response == null || !"SUCCESS".equals(response.status())) {
                String errorMessage = response != null ? response.errorMessage() : "응답이 없습니다.";
                log.warn("Report 동기 생성 실패 - mainReportId: {}, detailReportId: {}, error: {}",
                        savedMainReport.getId(), savedDetailReport.getId(), errorMessage);
                savedMainReport.failReport(errorMessage);
                savedDetailReport.failReport(errorMessage);
                throw new ReportException(ReportErrorCode.REPORT_GENERATION_FAILED);
            }

            // 4-2. content 존재 여부 검증
            JsonNode content = response.content();
            if (content == null || content.isNull()) {
                log.warn("Report content가 null - mainReportId: {}, detailReportId: {}",
                        savedMainReport.getId(), savedDetailReport.getId());
                savedMainReport.failReport("리포트 content가 null입니다.");
                savedDetailReport.failReport("리포트 content가 null입니다.");
                throw new ReportException(ReportErrorCode.INVALID_JSON_FORMAT);
            }

            // 4-3. main/detail 필드 검증
            JsonNode mainContent = content.get("main");
            JsonNode detailContent = content.get("detail");

            if (mainContent == null || mainContent.isNull() || detailContent == null || detailContent.isNull()) {
                log.warn("Report content가 비어있음 - mainReportId: {}, detailReportId: {}",
                        savedMainReport.getId(), savedDetailReport.getId());
                savedMainReport.failReport("리포트 content가 비어있습니다.");
                savedDetailReport.failReport("리포트 content가 비어있습니다.");
                throw new ReportException(ReportErrorCode.INVALID_JSON_FORMAT);
            }

            // 5. 리포트 완료 처리
            savedMainReport.completeReport(mainContent.toString());
            savedDetailReport.completeReport(detailContent.toString());

            // 6. 응답에서 techstacks 추출하여 DevTechstack AUTO로 저장
            saveAutoTechstacks(gitRepoUrl.getMember(), response.techstacks());

            log.info("Report 동기 생성 완료 - mainReportId: {}, detailReportId: {}",
                    savedMainReport.getId(), savedDetailReport.getId());

            return ReportConverter.toCreateReportSyncRes(savedMainReport, savedDetailReport, mainContent, detailContent);

        } catch (ReportException e) {
            // ReportException은 그대로 전파 (실패 상태는 이미 저장됨)
            throw e;
        } catch (Exception e) {
            // 예상치 못한 예외: 실패 상태 저장 후 ReportException으로 래핑
            log.error("Report 동기 생성 중 예외 발생 - mainReportId: {}, detailReportId: {}",
                    savedMainReport.getId(), savedDetailReport.getId(), e);
            savedMainReport.failReport(e.getMessage());
            savedDetailReport.failReport(e.getMessage());
            throw new ReportException(ReportErrorCode.REPORT_GENERATION_FAILED);
        }
    }

    @Override
    public void processCallback(ReportReqDTO.CallbackReq request) {
        DevReport mainReport = devReportRepository.findByIdWithMember(request.mainReportId())
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

                // techstacks 처리
                Member member = mainReport.getGitRepoUrl().getMember();
                saveAutoTechstacks(member, request.techstacks());

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
        if (devReportRepository.existsActiveReportByGitRepoUrlId(gitRepoId)) {
            log.warn("리포트 중복 생성 시도 - gitRepoId: {}", gitRepoId);
            throw new ReportException(ReportErrorCode.REPORT_ALREADY_EXISTS);
        }
    }

    // 리포트 저장 시 동시 요청으로 인한 중복 삽입을 처리 (partial unique index 위반 대응)
    private DevReport saveReportWithDuplicateCheck(DevReport report) {
        try {
            return devReportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            log.warn("리포트 중복 저장 시도 (동시 요청) - gitRepoId: {}, reportType: {}",
                    report.getGitRepoUrl().getId(), report.getReportType());
            throw new ReportException(ReportErrorCode.REPORT_ALREADY_EXISTS);
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
                throw new ReportException(ReportErrorCode.REPORT_ALREADY_EXISTS);
            }
        });
        // 현재 영속성 컨텍스트에 merge하여 이후 변경사항이 자동 저장되도록 함
        return devReportRepository.save(saved);
    }

    private void saveAutoTechstacks(Member member, List<String> techstackNames) {
        if (techstackNames == null || techstackNames.isEmpty()) {
            log.info("저장할 techstacks가 없습니다. memberId: {}", member.getId());
            return;
        }

        // String을 TechName enum으로 변환
        List<TechName> techNames = techstackNames.stream()
                .map(name -> {
                    try {
                        return TechName.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        log.warn("알 수 없는 TechName: {}", name);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        if (techNames.isEmpty()) {
            log.info("유효한 techstacks가 없습니다. memberId: {}", member.getId());
            return;
        }

        // Techstack 엔티티 조회 (parent fetch join)
        List<Techstack> techstacks = techstackRepository.findAllByNameInWithParent(techNames);

        if (techstacks.isEmpty()) {
            log.info("매칭되는 Techstack이 없습니다. memberId: {}", member.getId());
            return;
        }

        // 하위 techstack + parent를 모두 수집 (중복 제거)
        Set<Techstack> allTechstacks = new HashSet<>(techstacks);
        for (Techstack ts : techstacks) {
            if (ts.getParentStack() != null) {
                allTechstacks.add(ts.getParentStack());
            }
        }

        List<Techstack> techstackList = new ArrayList<>(allTechstacks);

        // 이미 존재하는 DevTechstack 조회 (fetch join으로 N+1 방지)
        List<DevTechstack> existingDevTechstacks = devTechstackRepository.findAllByMemberAndTechstackInWithTechstack(member, techstackList);
        Map<Long, DevTechstack> existingMap = existingDevTechstacks.stream()
                .collect(java.util.stream.Collectors.toMap(dt -> dt.getTechstack().getId(), dt -> dt));

        List<DevTechstack> toSave = new ArrayList<>();
        int updatedCount = 0;

        for (Techstack ts : techstackList) {
            DevTechstack existing = existingMap.get(ts.getId());

            if (existing == null) {
                // 새로 추가
                toSave.add(DevTechstack.builder()
                        .member(member)
                        .techstack(ts)
                        .source(TechstackSource.AUTO)
                        .build());
            } else if (existing.getSource() == TechstackSource.MANUAL) {
                // MANUAL → AUTO로 업데이트 (AUTO가 더 강한 권한)
                existing.updateSourceToAuto();
                updatedCount++;
            }
            // AUTO인 경우는 그대로 유지
        }

        if (!toSave.isEmpty()) {
            devTechstackRepository.saveAll(toSave);
            log.info("DevTechstack AUTO 신규 저장 - memberId: {}, count: {}", member.getId(), toSave.size());
        }

        if (updatedCount > 0) {
            log.info("DevTechstack MANUAL → AUTO 업데이트 - memberId: {}, count: {}", member.getId(), updatedCount);
        }
    }
}
