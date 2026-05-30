package com.umc.devine.domain.report.service.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.infrastructure.fastapi.FastApiReportClient;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * processCallback / handleReportFailed의 크레딧 환불 및 리포트 상태 변경을 검증하는 통합 테스트.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportCommandServiceCallbackTest extends IntegrationTestSupport {

    @Autowired
    private ReportCommandService reportCommandService;

    @Autowired
    private DevReportRepository devReportRepository;

    @Autowired
    private GitRepoUrlRepository gitRepoUrlRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberReportCreditRepository memberReportCreditRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 비동기 FastAPI 이벤트 리스너가 실제 HTTP 호출을 시도하지 않도록 mock 처리
    @MockitoBean
    private FastApiReportClient fastApiReportClient;

    private Member testMember;
    private GitRepoUrl testGitRepoUrl;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.saveAndFlush(Member.builder()
                .clerkId("clerk_callback_test")
                .name("콜백테스트")
                .nickname("callback_testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        testGitRepoUrl = gitRepoUrlRepository.saveAndFlush(GitRepoUrl.builder()
                .member(testMember)
                .gitUrl("https://github.com/test/callback-test-repo")
                .build());
    }

    @AfterEach
    void cleanUp() throws InterruptedException {
        Thread.sleep(500);
        jdbcTemplate.execute("DELETE FROM notification");
        devReportRepository.deleteAll();
        memberReportCreditRepository.findByMember(testMember)
                .ifPresent(memberReportCreditRepository::delete);
        gitRepoUrlRepository.deleteById(testGitRepoUrl.getId());
        memberRepository.deleteById(testMember.getId());
    }

    private DevReport createPendingReport(ReportType reportType) {
        return devReportRepository.saveAndFlush(DevReport.builder()
                .gitRepoUrl(testGitRepoUrl)
                .reportType(reportType)
                .build());
    }

    private DevReport createPendingReport(GitRepoUrl gitRepoUrl, ReportType reportType) {
        return devReportRepository.saveAndFlush(DevReport.builder()
                .gitRepoUrl(gitRepoUrl)
                .reportType(reportType)
                .build());
    }

    @Test
    @DisplayName("processCallback FAILED - 리포트가 FAILED로 마킹되고 크레딧이 환불된다")
    void processCallbackFailed_marksReportsFailedAndRefundsCredit() {
        // given - 크레딧이 이미 차감된 상태 (리포트 생성 시 차감됨)
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 0));
        DevReport mainReport = createPendingReport(ReportType.MAIN);
        DevReport detailReport = createPendingReport(ReportType.DETAIL);

        ReportReqDTO.CallbackReq request = ReportReqDTO.CallbackReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .status(ReportReqDTO.CallbackStatus.FAILED)
                .errorMessage("AI 서버 장애")
                .build();

        // when
        reportCommandService.processCallback(request);

        // then - 리포트 FAILED 마킹
        DevReport savedMain = devReportRepository.findById(mainReport.getId()).orElseThrow();
        DevReport savedDetail = devReportRepository.findById(detailReport.getId()).orElseThrow();
        assertThat(savedMain.getErrorMessage()).isEqualTo("AI 서버 장애");
        assertThat(savedDetail.getErrorMessage()).isEqualTo("AI 서버 장애");

        // then - 크레딧 환불 (0 → 1)
        MemberReportCredit credit = memberReportCreditRepository.findByMember(testMember).orElseThrow();
        assertThat(credit.getRemainingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("processCallback SUCCESS에서 content가 null이면 예외가 발생하고 크레딧이 환불된다")
    void processCallbackSuccess_withNullContent_refundsCredit() {
        // given
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 0));
        DevReport mainReport = createPendingReport(ReportType.MAIN);
        DevReport detailReport = createPendingReport(ReportType.DETAIL);

        ReportReqDTO.CallbackReq request = ReportReqDTO.CallbackReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .status(ReportReqDTO.CallbackStatus.SUCCESS)
                .content(null)
                .build();

        // when - content가 null이므로 예외 발생
        assertThatThrownBy(() -> reportCommandService.processCallback(request))
                .isInstanceOf(ReportException.class);

        // then - 크레딧 환불 (REQUIRES_NEW으로 커밋되어야 함)
        MemberReportCredit credit = memberReportCreditRepository.findByMember(testMember).orElseThrow();
        assertThat(credit.getRemainingCount()).isEqualTo(1);

        // then - 리포트가 FAILED 상태로 마킹되어야 함
        // NOTE: handleCallbackSuccess의 catch에서 throw e가 외부 @Transactional을 롤백시켜
        //       failReport() 변경이 DB에 반영되지 않는 버그(Bug #1)가 있음.
        //       이 assertion은 의도한 동작을 검증하며, 버그 수정 시 통과됨.
        DevReport savedMain = devReportRepository.findById(mainReport.getId()).orElseThrow();
        assertThat(savedMain.getErrorMessage()).isNotNull();
    }

    @Test
    @DisplayName("processCallback SUCCESS - content는 있으나 main/detail이 없으면 예외가 발생하고 리포트가 FAILED로 영속되며 크레딧이 환불된다")
    void processCallbackSuccess_withMissingMainDetail_persistsFailedAndRefundsCredit() {
        // given
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 0));
        DevReport mainReport = createPendingReport(ReportType.MAIN);
        DevReport detailReport = createPendingReport(ReportType.DETAIL);

        // content는 존재하지만 main/detail 필드가 누락된 잘못된 형식
        ObjectNode content = new ObjectMapper().createObjectNode();
        content.put("unexpected", "no main/detail field");

        ReportReqDTO.CallbackReq request = ReportReqDTO.CallbackReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .status(ReportReqDTO.CallbackStatus.SUCCESS)
                .content(content)
                .build();

        // when - 잘못된 content이므로 예외 발생
        assertThatThrownBy(() -> reportCommandService.processCallback(request))
                .isInstanceOf(ReportException.class);

        // then - 예외가 던져지더라도 리포트는 FAILED 상태로 DB에 영속되어야 한다 (바깥 트랜잭션 롤백에도 살아남아야 함)
        DevReport savedMain = devReportRepository.findById(mainReport.getId()).orElseThrow();
        DevReport savedDetail = devReportRepository.findById(detailReport.getId()).orElseThrow();
        assertThat(savedMain.getErrorMessage()).isNotNull();
        assertThat(savedDetail.getErrorMessage()).isNotNull();

        // then - 크레딧 환불 (0 → 1)
        MemberReportCredit credit = memberReportCreditRepository.findByMember(testMember).orElseThrow();
        assertThat(credit.getRemainingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("processCallback SUCCESS - 잘못된 콜백을 두 번 호출해도 환불은 한 번만 되고 리포트는 FAILED로 유지된다")
    void processCallbackSuccess_invalidContentCalledTwice_refundsOnlyOnce() {
        // given
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 0));
        DevReport mainReport = createPendingReport(ReportType.MAIN);
        DevReport detailReport = createPendingReport(ReportType.DETAIL);

        ReportReqDTO.CallbackReq request = ReportReqDTO.CallbackReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .status(ReportReqDTO.CallbackStatus.SUCCESS)
                .content(null)
                .build();

        // when - 동일한 실패 콜백을 두 번 호출 (예외는 무시)
        catchThrowable(() -> reportCommandService.processCallback(request));
        catchThrowable(() -> reportCommandService.processCallback(request));

        // then - 환불은 한 번만 적용되어야 한다 (0 → 1, 이중 환불 시 2가 됨)
        MemberReportCredit credit = memberReportCreditRepository.findByMember(testMember).orElseThrow();
        assertThat(credit.getRemainingCount()).isEqualTo(1);

        // then - 리포트는 FAILED 상태로 유지되어 멱등성 가드가 작동해야 한다
        DevReport savedMain = devReportRepository.findById(mainReport.getId()).orElseThrow();
        assertThat(savedMain.getErrorMessage()).isNotNull();
    }

    @Test
    @DisplayName("processCallback FAILED - 동일 콜백이 동시에 들어와도 환불은 한 번만 적용된다")
    void processCallbackFailed_concurrentDuplicateCallbacks_refundsOnlyOnce() throws Exception {
        // given
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 0));
        DevReport mainReport = createPendingReport(ReportType.MAIN);
        DevReport detailReport = createPendingReport(ReportType.DETAIL);

        ReportReqDTO.CallbackReq request = ReportReqDTO.CallbackReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .status(ReportReqDTO.CallbackStatus.FAILED)
                .errorMessage("AI 서버 장애")
                .build();

        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Throwable>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await();
                return catchThrowable(() -> reportCommandService.processCallback(request));
            });
        }

        // when
        var futures = tasks.stream().map(executor::submit).toList();
        ready.await();
        start.countDown();
        for (var future : futures) {
            assertThat(future.get()).isNull();
        }
        executor.shutdown();

        // then
        MemberReportCredit credit = memberReportCreditRepository.findByMember(testMember).orElseThrow();
        assertThat(credit.getRemainingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("processCallback SUCCESS - 환불이 불가능하면 FAILED 마킹도 커밋하지 않는다")
    void processCallbackSuccess_whenRefundCannotBeApplied_doesNotPersistFailedMark() {
        // given - 크레딧 행이 없어 환불 업데이트가 0건이 되는 비정상 상태
        DevReport mainReport = createPendingReport(ReportType.MAIN);
        DevReport detailReport = createPendingReport(ReportType.DETAIL);

        ReportReqDTO.CallbackReq request = ReportReqDTO.CallbackReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(detailReport.getId())
                .status(ReportReqDTO.CallbackStatus.SUCCESS)
                .content(null)
                .build();

        // when
        assertThatThrownBy(() -> reportCommandService.processCallback(request))
                .isInstanceOf(Exception.class);

        // then - 실패 상태와 환불이 한 트랜잭션이어야 하므로, 환불 실패 시 FAILED 마킹도 남지 않아야 한다
        DevReport savedMain = devReportRepository.findById(mainReport.getId()).orElseThrow();
        DevReport savedDetail = devReportRepository.findById(detailReport.getId()).orElseThrow();
        assertThat(savedMain.getErrorMessage()).isNull();
        assertThat(savedDetail.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("processCallback SUCCESS - main/detail이 같은 저장소의 리포트 쌍이 아니면 처리하지 않는다")
    void processCallbackSuccess_rejectsMismatchedReportPair() {
        // given
        Member otherMember = memberRepository.saveAndFlush(Member.builder()
                .clerkId("clerk_callback_other")
                .name("다른회원")
                .nickname("callback_otheruser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());
        GitRepoUrl otherGitRepoUrl = gitRepoUrlRepository.saveAndFlush(GitRepoUrl.builder()
                .member(otherMember)
                .gitUrl("https://github.com/test/other-callback-test-repo")
                .build());

        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 0));
        DevReport mainReport = createPendingReport(testGitRepoUrl, ReportType.MAIN);
        DevReport mismatchedDetailReport = createPendingReport(otherGitRepoUrl, ReportType.DETAIL);

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode content = objectMapper.createObjectNode();
        content.set("main", objectMapper.createObjectNode().put("summary", "Main report"));
        content.set("detail", objectMapper.createObjectNode().put("summary", "Detail report"));

        ReportReqDTO.CallbackReq request = ReportReqDTO.CallbackReq.builder()
                .mainReportId(mainReport.getId())
                .detailReportId(mismatchedDetailReport.getId())
                .status(ReportReqDTO.CallbackStatus.SUCCESS)
                .content(content)
                .build();

        try {
            // when
            assertThatThrownBy(() -> reportCommandService.processCallback(request))
                    .isInstanceOf(ReportException.class);

            // then
            DevReport savedMain = devReportRepository.findById(mainReport.getId()).orElseThrow();
            DevReport savedDetail = devReportRepository.findById(mismatchedDetailReport.getId()).orElseThrow();
            assertThat(savedMain.getCompletedAt()).isNull();
            assertThat(savedDetail.getCompletedAt()).isNull();
        } finally {
            devReportRepository.deleteById(mismatchedDetailReport.getId());
            gitRepoUrlRepository.deleteById(otherGitRepoUrl.getId());
            memberRepository.deleteById(otherMember.getId());
        }
    }

    @Test
    @DisplayName("[재현] handleReportFailed - 이미 완료(SUCCESS)된 리포트는 삭제하거나 환불하지 않아야 한다")
    void handleReportFailed_whenReportsAlreadyCompleted_shouldNotDeleteOrRefund() {
        // given - 크레딧 1이 차감된 상태(리포트 생성 시 차감)에서, 성공 콜백이 먼저 처리되어
        //         두 리포트가 이미 완료(completedAt 설정)된 상황.
        //         이는 "FastAPI POST 읽기 타임아웃(RestClientException)이 났지만 서버는 정상 처리하여
        //          SUCCESS 콜백이 handleReportFailed의 findById~delete 사이에 완료를 커밋한" 경쟁의 결과 상태와 동일하다.
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 0));
        DevReport mainReport = createPendingReport(ReportType.MAIN);
        DevReport detailReport = createPendingReport(ReportType.DETAIL);
        mainReport.completeReport("{\"summary\":\"main report\"}");
        detailReport.completeReport("{\"summary\":\"detail report\"}");
        devReportRepository.saveAndFlush(mainReport);
        devReportRepository.saveAndFlush(detailReport);

        // when - 뒤늦게 handleReportFailed가 호출됨 (POST 예외 처리 경로)
        reportCommandService.handleReportFailed(
                mainReport.getId(), detailReport.getId(),
                testMember.getId(),
                testGitRepoUrl.getGitUrl(),
                "FastAPI 호출 실패: read timeout"
        );

        // then - 이미 완료된 리포트는 보존되어야 한다
        //        (현재 구현은 상태 무검사로 deleteById하여 성공 리포트를 삭제 → 이 단언에서 실패하며 결함이 재현됨)
        assertThat(devReportRepository.findById(mainReport.getId()))
                .withFailMessage("완료된 main 리포트가 handleReportFailed에 의해 삭제되었습니다 (데이터 손실)")
                .isPresent();
        assertThat(devReportRepository.findById(detailReport.getId()))
                .withFailMessage("완료된 detail 리포트가 handleReportFailed에 의해 삭제되었습니다 (데이터 손실)")
                .isPresent();

        // then - 사용자가 리포트를 정상 수령했으므로 크레딧은 환불되면 안 된다
        //        (현재 구현은 무조건 refundCredit → 0에서 1로 오환불되어 이 단언에서 실패)
        MemberReportCredit credit = memberReportCreditRepository.findByMember(testMember).orElseThrow();
        assertThat(credit.getRemainingCount())
                .withFailMessage("완료된 리포트에 대해 크레딧이 오환불되었습니다 (실제: %d, 기대: 0)", credit.getRemainingCount())
                .isEqualTo(0);
    }

    @Test
    @DisplayName("handleReportFailed - 회원이 존재하지 않으면 리포트만 삭제되고 크레딧 환불은 생략된다")
    void handleReportFailed_whenMemberNotFound_onlyDeletesReports() {
        // given
        DevReport mainReport = createPendingReport(ReportType.MAIN);
        DevReport detailReport = createPendingReport(ReportType.DETAIL);
        long nonExistentMemberId = -999L;

        // when - 예외 없이 완료되어야 한다
        reportCommandService.handleReportFailed(
                mainReport.getId(), detailReport.getId(),
                nonExistentMemberId,
                "https://github.com/test/repo",
                "GitHub 토큰 조회 실패"
        );

        // then - 리포트 삭제됨
        assertThat(devReportRepository.findById(mainReport.getId())).isEmpty();
        assertThat(devReportRepository.findById(detailReport.getId())).isEmpty();
    }
}
