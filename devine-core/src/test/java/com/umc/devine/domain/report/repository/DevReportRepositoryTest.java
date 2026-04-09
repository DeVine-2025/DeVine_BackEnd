package com.umc.devine.domain.report.repository;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.GitRepoUrlRepository;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DevReportRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private DevReportRepository devReportRepository;

    @Autowired
    private GitRepoUrlRepository gitRepoUrlRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member testMember;
    private GitRepoUrl gitRepoUrl1;
    private GitRepoUrl gitRepoUrl2;
    private GitRepoUrl gitRepoUrl3;

    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.builder()
                .clerkId("clerk_test_123")
                .name("테스트")
                .nickname("testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        gitRepoUrl1 = gitRepoUrlRepository.save(GitRepoUrl.builder()
                .member(testMember)
                .gitUrl("https://github.com/user/repo1")
                .gitDescription("레포1 설명")
                .build());

        gitRepoUrl2 = gitRepoUrlRepository.save(GitRepoUrl.builder()
                .member(testMember)
                .gitUrl("https://github.com/user/repo2")
                .gitDescription("레포2 설명")
                .build());

        gitRepoUrl3 = gitRepoUrlRepository.save(GitRepoUrl.builder()
                .member(testMember)
                .gitUrl("https://github.com/user/repo3")
                .gitDescription("레포3 설명")
                .build());
    }

    @Nested
    @DisplayName("findActiveReportGitRepoIds")
    class FindActiveReportGitRepoIdsTest {

        @Test
        @DisplayName("활성 리포트가 있는 레포 ID만 반환한다")
        void findActiveReportGitRepoIds_returnsOnlyActiveReports() {
            // given
            // repo1: 활성 리포트 있음 (errorMessage = null)
            devReportRepository.save(DevReport.builder()
                    .gitRepoUrl(gitRepoUrl1)
                    .reportType(ReportType.MAIN)
                    .build());

            // repo2: 실패한 리포트만 있음 (errorMessage != null)
            DevReport failedReport = DevReport.builder()
                    .gitRepoUrl(gitRepoUrl2)
                    .reportType(ReportType.MAIN)
                    .build();
            failedReport.failReport("분석 실패");
            devReportRepository.save(failedReport);

            // repo3: 리포트 없음

            List<Long> gitRepoIds = List.of(gitRepoUrl1.getId(), gitRepoUrl2.getId(), gitRepoUrl3.getId());

            // when
            List<Long> result = devReportRepository.findActiveReportGitRepoIds(gitRepoIds);

            // then
            assertThat(result).containsExactly(gitRepoUrl1.getId());
        }

        @Test
        @DisplayName("모든 레포에 활성 리포트가 있으면 모두 반환한다")
        void findActiveReportGitRepoIds_returnsAllWhenAllActive() {
            // given
            devReportRepository.save(DevReport.builder()
                    .gitRepoUrl(gitRepoUrl1)
                    .reportType(ReportType.MAIN)
                    .build());

            devReportRepository.save(DevReport.builder()
                    .gitRepoUrl(gitRepoUrl2)
                    .reportType(ReportType.MAIN)
                    .build());

            List<Long> gitRepoIds = List.of(gitRepoUrl1.getId(), gitRepoUrl2.getId());

            // when
            List<Long> result = devReportRepository.findActiveReportGitRepoIds(gitRepoIds);

            // then
            assertThat(result).containsExactlyInAnyOrder(gitRepoUrl1.getId(), gitRepoUrl2.getId());
        }

        @Test
        @DisplayName("활성 리포트가 없으면 빈 리스트를 반환한다")
        void findActiveReportGitRepoIds_returnsEmptyWhenNoActiveReports() {
            // given
            DevReport failedReport1 = DevReport.builder()
                    .gitRepoUrl(gitRepoUrl1)
                    .reportType(ReportType.MAIN)
                    .build();
            failedReport1.failReport("분석 실패");
            devReportRepository.save(failedReport1);

            List<Long> gitRepoIds = List.of(gitRepoUrl1.getId(), gitRepoUrl2.getId());

            // when
            List<Long> result = devReportRepository.findActiveReportGitRepoIds(gitRepoIds);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 리스트를 입력하면 빈 리스트를 반환한다")
        void findActiveReportGitRepoIds_returnsEmptyForEmptyInput() {
            // given
            List<Long> emptyList = List.of();

            // when
            List<Long> result = devReportRepository.findActiveReportGitRepoIds(emptyList);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("동일 레포에 여러 리포트가 있어도 중복 없이 반환한다")
        void findActiveReportGitRepoIds_returnsDistinctIds() {
            // given
            // repo1에 MAIN, DETAIL 두 개의 활성 리포트
            devReportRepository.save(DevReport.builder()
                    .gitRepoUrl(gitRepoUrl1)
                    .reportType(ReportType.MAIN)
                    .build());

            devReportRepository.save(DevReport.builder()
                    .gitRepoUrl(gitRepoUrl1)
                    .reportType(ReportType.DETAIL)
                    .build());

            List<Long> gitRepoIds = List.of(gitRepoUrl1.getId());

            // when
            List<Long> result = devReportRepository.findActiveReportGitRepoIds(gitRepoIds);

            // then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactly(gitRepoUrl1.getId());
        }
    }
}
