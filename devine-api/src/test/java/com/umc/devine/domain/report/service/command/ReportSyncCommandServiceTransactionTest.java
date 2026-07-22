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
import com.umc.devine.domain.report.exception.ReportException;
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.infrastructure.fastapi.FastApiSyncReportClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * createReportSync의 트랜잭션 동작을 검증하는 통합 테스트.
 *
 * 핵심 검증 포인트:
 * - 준비 구간(검증·크레딧 차감·리포트 저장)이 한 트랜잭션으로 묶여 실패 시 함께 롤백되는지
 * - FastAPI 실패 시 리포트 행 삭제와 크레딧 환불이 함께 반영되는지
 * - Partial Unique Index로 실패한 리포트가 재시도 가능한지
 *
 * @Transactional(propagation = NOT_SUPPORTED)를 사용하여
 * 테스트 레벨 트랜잭션을 비활성화하고, 서비스의 실제 트랜잭션 경계를 테스트합니다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportSyncCommandServiceTransactionTest extends IntegrationTestSupport {

    @Autowired
    private ReportSyncCommandService reportSyncCommandService;

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

    @MockitoBean
    private FastApiSyncReportClient fastApiSyncReportClient;

    private Member testMember;
    private GitRepoUrl testGitRepoUrl;

    @BeforeEach
    void setUp() {
        // Partial Unique Index 생성 (ddl-auto로는 생성되지 않으므로 직접 실행, IF NOT EXISTS로 멱등성 보장)
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_report_active_per_repo_type
                    ON dev_report(git_repo_id, report_type)
                    WHERE error_message IS NULL
                """);

        testMember = memberRepository.saveAndFlush(Member.builder()
                .clerkId("clerk_report_sync_tx_test")
                .name("테스트")
                .nickname("report_sync_tx_user")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 1));

        testGitRepoUrl = gitRepoUrlRepository.saveAndFlush(GitRepoUrl.builder()
                .member(testMember)
                .gitUrl("https://github.com/test/sync-tx-test-repo")
                .build());
    }

    @AfterEach
    void cleanupTestData() throws InterruptedException {
        // 비동기 알림 생성(@Async + AFTER_COMMIT)이 완료될 때까지 대기
        Thread.sleep(500);
        jdbcTemplate.execute("DELETE FROM notification");
        devReportRepository.deleteAll();
        memberReportCreditRepository.findByMember(testMember).ifPresent(memberReportCreditRepository::delete);
        gitRepoUrlRepository.deleteById(testGitRepoUrl.getId());
        memberRepository.deleteById(testMember.getId());
    }

    private ReportReqDTO.CreateReportReq createRequest() {
        return ReportReqDTO.CreateReportReq.builder()
                .gitRepoId(testGitRepoUrl.getId())
                .build();
    }

    private int remainingCredit() {
        return memberReportCreditRepository.findByMember(testMember)
                .map(MemberReportCredit::getRemainingCount).orElse(0);
    }

    private FastApiResDto.ReportGenerationSyncRes successResponse() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode content = objectMapper.createObjectNode();
        content.set("main", objectMapper.createObjectNode().put("summary", "Main report"));
        content.set("detail", objectMapper.createObjectNode().put("summary", "Detail report"));

        return FastApiResDto.ReportGenerationSyncRes.builder()
                .status("SUCCESS")
                .content(content)
                .build();
    }

    @Nested
    @DisplayName("createReportSync 트랜잭션 경계 테스트")
    class CreateReportSyncTransactionTest {

        @Test
        @DisplayName("FastAPI 실패 응답 시 리포트가 삭제되고 크레딧이 환불된다")
        void createReportSync_FastAPI실패응답시_리포트가_삭제되고_크레딧이_환불된다() {
            // given
            int creditBefore = remainingCredit();

            given(fastApiSyncReportClient.requestReportGenerationSync(
                    anyLong(), anyLong(), anyString(), anyString()))
                    .willReturn(FastApiResDto.ReportGenerationSyncRes.builder()
                            .status("FAILED")
                            .errorMessage("AI 서버 오류")
                            .build());

            // when
            assertThatThrownBy(() ->
                    reportSyncCommandService.createReportSync(testMember.getId(), createRequest()))
                    .isInstanceOf(ReportException.class);

            // then - 실패 시 리포트 삭제
            assertThat(devReportRepository.findAll()).isEmpty();

            // then - 크레딧 환불
            assertThat(remainingCredit()).isEqualTo(creditBefore);
        }

        @Test
        @DisplayName("예상치 못한 예외 발생 시 리포트가 삭제되고 크레딧이 환불된다")
        void createReportSync_예상치못한예외시_리포트가_삭제되고_크레딧이_환불된다() {
            // given
            int creditBefore = remainingCredit();

            given(fastApiSyncReportClient.requestReportGenerationSync(
                    anyLong(), anyLong(), anyString(), anyString()))
                    .willThrow(new RuntimeException("Connection timeout"));

            // when
            assertThatThrownBy(() ->
                    reportSyncCommandService.createReportSync(testMember.getId(), createRequest()))
                    .isInstanceOf(ReportException.class);

            // then - 실패 시 리포트 삭제
            assertThat(devReportRepository.findAll()).isEmpty();

            // then - 크레딧 환불
            assertThat(remainingCredit()).isEqualTo(creditBefore);
        }

        @Test
        @DisplayName("FastAPI 성공 시 리포트가 완료 상태로 DB에 저장되고 크레딧이 차감된다")
        void createReportSync_성공시_리포트가_완료상태로_DB에_저장된다() {
            // given
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    anyLong(), anyLong(), anyString(), anyString()))
                    .willReturn(successResponse());

            // when
            reportSyncCommandService.createReportSync(testMember.getId(), createRequest());

            // then
            List<DevReport> reports = devReportRepository.findAll();
            assertThat(reports).hasSize(2);
            assertThat(reports).allMatch(r -> r.getCompletedAt() != null);
            assertThat(reports).allMatch(r -> r.getErrorMessage() == null);
            assertThat(reports).allMatch(r -> r.getContent() != null);

            assertThat(remainingCredit()).isZero();
        }

        @Test
        @DisplayName("이미 활성 리포트가 있으면 크레딧이 차감되지 않는다")
        void createReportSync_중복요청시_크레딧이_차감되지_않는다() {
            // given - 1차 요청 성공으로 활성 리포트 2건 생성
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    anyLong(), anyLong(), anyString(), anyString()))
                    .willReturn(successResponse());
            reportSyncCommandService.createReportSync(testMember.getId(), createRequest());

            // 크레딧 소진 때문이 아니라 중복 때문에 거절되는지 보기 위해 크레딧을 채워둔다
            jdbcTemplate.update("UPDATE member_report_credit SET remaining_count = 1 WHERE member_id = ?",
                    testMember.getId());
            int creditBefore = remainingCredit();

            // when - 같은 저장소로 재요청
            assertThatThrownBy(() ->
                    reportSyncCommandService.createReportSync(testMember.getId(), createRequest()))
                    .isInstanceOf(ReportException.class);

            // then - 리포트도 크레딧도 그대로
            assertThat(devReportRepository.findAll()).hasSize(2);
            assertThat(remainingCredit()).isEqualTo(creditBefore);
        }
    }

    @Nested
    @DisplayName("실패 후 재시도 테스트")
    class RetryAfterFailureTest {

        @Test
        @DisplayName("실패 후 재시도 시 새 리포트가 성공적으로 생성된다")
        void createReportSync_실패후_재시도하면_새리포트가_생성된다() {
            // given - 1차 시도: 실패 (리포트 삭제됨)
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    anyLong(), anyLong(), anyString(), anyString()))
                    .willReturn(FastApiResDto.ReportGenerationSyncRes.builder()
                            .status("FAILED")
                            .errorMessage("1차 실패")
                            .build());

            assertThatThrownBy(() ->
                    reportSyncCommandService.createReportSync(testMember.getId(), createRequest()))
                    .isInstanceOf(ReportException.class);

            assertThat(devReportRepository.findAll()).isEmpty();

            // given - 2차 시도: 성공
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    anyLong(), anyLong(), anyString(), anyString()))
                    .willReturn(successResponse());

            // when
            reportSyncCommandService.createReportSync(testMember.getId(), createRequest());

            // then - 성공 리포트 2개만 존재
            List<DevReport> allReports = devReportRepository.findAll();
            assertThat(allReports).hasSize(2);
            assertThat(allReports).allMatch(r -> r.getCompletedAt() != null && r.getErrorMessage() == null);
        }
    }
}
