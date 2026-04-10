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
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.ticket.entity.MemberReportCredit;
import com.umc.devine.domain.ticket.exception.TicketException;
import com.umc.devine.domain.ticket.exception.code.TicketErrorReason;
import com.umc.devine.domain.ticket.repository.MemberReportCreditRepository;
import com.umc.devine.infrastructure.fastapi.FastApiSyncReportClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * createReportSync의 크레딧 차감/환불 동작을 검증하는 통합 테스트.
 *
 * @Transactional(propagation = NOT_SUPPORTED)를 사용하여
 * 서비스의 실제 트랜잭션 경계를 테스트합니다.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportCommandServiceCreditTest extends IntegrationTestSupport {

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

    @MockitoBean
    private FastApiSyncReportClient fastApiSyncReportClient;

    private Member testMember;
    private GitRepoUrl testGitRepoUrl;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_report_active_per_repo_type
                    ON dev_report(git_repo_id, report_type)
                    WHERE error_message IS NULL
                """);

        testMember = memberRepository.saveAndFlush(Member.builder()
                .clerkId("clerk_credit_test")
                .name("크레딧테스트")
                .nickname("credit_testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        testGitRepoUrl = gitRepoUrlRepository.saveAndFlush(GitRepoUrl.builder()
                .member(testMember)
                .gitUrl("https://github.com/test/credit-test-repo")
                .build());
    }

    @AfterEach
    void cleanupTestData() throws InterruptedException {
        Thread.sleep(500);
        jdbcTemplate.execute("DELETE FROM notification");
        devReportRepository.deleteAll();
        memberReportCreditRepository.findByMember(testMember)
                .ifPresent(memberReportCreditRepository::delete);
        gitRepoUrlRepository.deleteById(testGitRepoUrl.getId());
        memberRepository.deleteById(testMember.getId());
    }

    private ReportReqDTO.CreateReportReq createRequest() {
        return ReportReqDTO.CreateReportReq.builder()
                .gitRepoId(testGitRepoUrl.getId())
                .build();
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

    @Test
    @DisplayName("크레딧이 0이면 TicketException(INSUFFICIENT_CREDITS)이 발생하고 리포트가 생성되지 않는다")
    void throwsWhenNoCredits() {
        // given
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 0));

        // when & then
        assertThatThrownBy(() ->
                reportCommandService.createReportSync(testMember.getId(), createRequest()))
                .isInstanceOf(TicketException.class)
                .satisfies(ex -> {
                    TicketException ticketEx = (TicketException) ex;
                    assertThat(ticketEx.getReason()).isEqualTo(TicketErrorReason.INSUFFICIENT_CREDITS);
                });

        // 리포트가 생성되지 않아야 함
        assertThat(devReportRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("FastAPI가 FAILED를 반환하면 예외가 발생하고 크레딧이 환불된다")
    void refundsOnFastApiFailure() {
        // given
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 1));

        given(fastApiSyncReportClient.requestReportGenerationSync(
                any(), any(), anyString(), anyString()))
                .willReturn(FastApiResDto.ReportGenerationSyncRes.builder()
                        .status("FAILED")
                        .errorMessage("AI 서버 오류")
                        .build());

        // when
        assertThatThrownBy(() ->
                reportCommandService.createReportSync(testMember.getId(), createRequest()))
                .isInstanceOf(Exception.class);

        // then - 크레딧이 환불되어 1로 복구되어야 함
        MemberReportCredit credit = memberReportCreditRepository.findByMember(testMember)
                .orElseThrow();
        assertThat(credit.getRemainingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("FastAPI 성공 시 크레딧이 1 차감된다")
    void deductsOnSuccess() {
        // given
        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(testMember, 2));

        given(fastApiSyncReportClient.requestReportGenerationSync(
                any(), any(), anyString(), anyString()))
                .willReturn(successResponse());

        // when
        reportCommandService.createReportSync(testMember.getId(), createRequest());

        // then - 크레딧이 1 차감되어 1이 남아야 함
        MemberReportCredit credit = memberReportCreditRepository.findByMember(testMember)
                .orElseThrow();
        assertThat(credit.getRemainingCount()).isEqualTo(1);
    }
}
