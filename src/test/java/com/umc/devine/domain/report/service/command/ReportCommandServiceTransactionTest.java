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
import com.umc.devine.infrastructure.fastapi.FastApiSyncReportClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * createReportSync의 트랜잭션 동작을 검증하는 통합 테스트.
 *
 * 핵심 검증 포인트:
 * - REQUIRES_NEW로 리포트 저장이 별도 트랜잭션에서 커밋되는지
 * - noRollbackFor(ReportException.class)로 실패 상태가 DB에 영속되는지
 *
 * @Transactional(propagation = NOT_SUPPORTED)를 사용하여
 * 테스트 레벨 트랜잭션을 비활성화하고, 서비스의 실제 트랜잭션 경계를 테스트합니다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportCommandServiceTransactionTest extends IntegrationTestSupport {

    @Autowired
    private ReportCommandService reportCommandService;

    @Autowired
    private DevReportRepository devReportRepository;

    @Autowired
    private GitRepoUrlRepository gitRepoUrlRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
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
                .clerkId("clerk_report_tx_test")
                .name("테스트")
                .nickname("report_tx_testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        testGitRepoUrl = gitRepoUrlRepository.saveAndFlush(GitRepoUrl.builder()
                .member(testMember)
                .gitUrl("https://github.com/test/tx-test-repo")
                .build());
    }

    @AfterEach
    void cleanupTestData() {
        devReportRepository.deleteAll();
        gitRepoUrlRepository.deleteById(testGitRepoUrl.getId());
        memberRepository.deleteById(testMember.getId());
    }

    @Nested
    @DisplayName("createReportSync 트랜잭션 오염 방지 테스트")
    class CreateReportSyncTransactionTest {

        @Test
        @DisplayName("FastAPI 실패 응답 시 리포트가 실패 상태로 DB에 저장된다")
        void createReportSync_FastAPI실패응답시_리포트가_실패상태로_DB에_저장된다() {
            // given
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(FastApiResDto.ReportGenerationSyncRes.builder()
                            .status("FAILED")
                            .errorMessage("AI 서버 오류")
                            .build());

            ReportReqDTO.CreateReportReq request = ReportReqDTO.CreateReportReq.builder()
                    .gitRepoId(testGitRepoUrl.getId())
                    .build();

            // when
            assertThatThrownBy(() ->
                    reportCommandService.createReportSync(testMember.getId(), request))
                    .isInstanceOf(ReportException.class);

            // then - noRollbackFor 덕분에 failReport() 변경사항이 커밋되어야 함
            List<DevReport> reports = devReportRepository.findAll();
            assertThat(reports).hasSize(2);
            assertThat(reports).allMatch(r -> r.getErrorMessage() != null);
            assertThat(reports).allMatch(r -> "AI 서버 오류".equals(r.getErrorMessage()));
        }

        @Test
        @DisplayName("예상치 못한 예외 발생 시에도 리포트가 실패 상태로 DB에 저장된다")
        void createReportSync_예상치못한예외시_리포트가_실패상태로_DB에_저장된다() {
            // given
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willThrow(new RuntimeException("Connection timeout"));

            ReportReqDTO.CreateReportReq request = ReportReqDTO.CreateReportReq.builder()
                    .gitRepoId(testGitRepoUrl.getId())
                    .build();

            // when
            assertThatThrownBy(() ->
                    reportCommandService.createReportSync(testMember.getId(), request))
                    .isInstanceOf(ReportException.class);

            // then - 예상치 못한 예외도 failReport 후 커밋
            List<DevReport> reports = devReportRepository.findAll();
            assertThat(reports).hasSize(2);
            assertThat(reports).allMatch(r -> r.getErrorMessage() != null);
            assertThat(reports).allMatch(r -> "Connection timeout".equals(r.getErrorMessage()));
        }

        @Test
        @DisplayName("FastAPI 성공 시 리포트가 완료 상태로 DB에 저장된다")
        void createReportSync_성공시_리포트가_완료상태로_DB에_저장된다() {
            // given
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode content = objectMapper.createObjectNode();
            content.set("main", objectMapper.createObjectNode().put("summary", "Main report"));
            content.set("detail", objectMapper.createObjectNode().put("summary", "Detail report"));

            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(FastApiResDto.ReportGenerationSyncRes.builder()
                            .status("SUCCESS")
                            .content(content)
                            .build());

            ReportReqDTO.CreateReportReq request = ReportReqDTO.CreateReportReq.builder()
                    .gitRepoId(testGitRepoUrl.getId())
                    .build();

            // when
            reportCommandService.createReportSync(testMember.getId(), request);

            // then
            List<DevReport> reports = devReportRepository.findAll();
            assertThat(reports).hasSize(2);
            assertThat(reports).allMatch(r -> r.getCompletedAt() != null);
            assertThat(reports).allMatch(r -> r.getErrorMessage() == null);
            assertThat(reports).allMatch(r -> r.getContent() != null);
        }
    }
}
