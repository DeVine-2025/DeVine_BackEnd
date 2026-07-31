package com.umc.devine.domain.report.service.command;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportCommandServiceCreateTest extends IntegrationTestSupport {

    @Autowired
    private ReportCommandService reportCommandService;

    @Autowired
    private DevReportRepository devReportRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GitRepoUrlRepository gitRepoUrlRepository;

    @Autowired
    private MemberReportCreditRepository memberReportCreditRepository;

    @MockitoBean
    private FastApiReportClient fastApiReportClient;

    private Member member;
    private GitRepoUrl gitRepoUrl;

    @BeforeEach
    void setUp() {
        member = memberRepository.saveAndFlush(Member.builder()
                .clerkId("clerk_report_create")
                .name("리포트생성")
                .nickname("report_create_user")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        gitRepoUrl = gitRepoUrlRepository.saveAndFlush(GitRepoUrl.builder()
                .member(member)
                .gitUrl("https://github.com/test/report-create-repo")
                .build());

        memberReportCreditRepository.saveAndFlush(MemberReportCredit.of(member, 1));
    }

    @AfterEach
    void tearDown() {
        devReportRepository.deleteAll();
        memberReportCreditRepository.findByMember(member).ifPresent(memberReportCreditRepository::delete);
        gitRepoUrlRepository.deleteById(gitRepoUrl.getId());
        memberRepository.deleteById(member.getId());
    }

    @Test
    @DisplayName("createReport - 크레딧 차감과 리포트 생성이 같은 요청에서 성공한다")
    void createReport_deductsCreditAndCreatesPendingReports() {
        ReportReqDTO.CreateReportReq request = ReportReqDTO.CreateReportReq.builder()
                .gitRepoId(gitRepoUrl.getId())
                .build();

        reportCommandService.createReport(member.getId(), request);

        List<DevReport> reports = devReportRepository.findAll();
        assertThat(reports).hasSize(2);
        assertThat(reports).extracting(DevReport::getReportType)
                .containsExactlyInAnyOrder(ReportType.MAIN, ReportType.DETAIL);
        assertThat(reports).allSatisfy(report -> {
            assertThat(report.getCompletedAt()).isNull();
            assertThat(report.getErrorMessage()).isNull();
        });

        MemberReportCredit credit = memberReportCreditRepository.findByMember(member).orElseThrow();
        assertThat(credit.getRemainingCount()).isZero();
    }
}
