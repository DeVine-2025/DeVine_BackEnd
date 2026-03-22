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
import com.umc.devine.domain.report.repository.DevReportRepository;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.DevTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.enums.TechstackSource;
import com.umc.devine.domain.techstack.repository.DevTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.infrastructure.fastapi.FastApiSyncReportClient;
import com.umc.devine.infrastructure.fastapi.dto.FastApiResDto;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * createReportSync 및 processCallback의 techstacks 처리를 검증하는 통합 테스트.
 *
 * 핵심 검증 포인트:
 * - FastAPI 응답의 techstacks가 DevTechstack으로 저장되는지
 * - 하위 techstack 추가 시 parent techstack도 함께 추가되는지
 * - MANUAL 소스가 AUTO로 업데이트되는지 (AUTO > MANUAL 우선순위)
 * - null/empty techstacks 처리 시 오류 없이 진행되는지
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReportCommandServiceTechstackTest extends IntegrationTestSupport {

    @Autowired
    private ReportCommandService reportCommandService;

    @Autowired
    private DevReportRepository devReportRepository;

    @Autowired
    private GitRepoUrlRepository gitRepoUrlRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TechstackRepository techstackRepository;

    @Autowired
    private DevTechstackRepository devTechstackRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private FastApiSyncReportClient fastApiSyncReportClient;

    private Member testMember;
    private GitRepoUrl testGitRepoUrl;
    private Techstack backendStack;
    private Techstack javaStack;
    private Techstack springbootStack;

    @BeforeEach
    void setUp() {
        // Partial Unique Index 생성
        jdbcTemplate.execute("""
                CREATE UNIQUE INDEX IF NOT EXISTS uk_dev_report_active_per_repo_type
                    ON dev_report(git_repo_id, report_type)
                    WHERE error_message IS NULL
                """);

        testMember = memberRepository.saveAndFlush(Member.builder()
                .clerkId("clerk_techstack_test")
                .name("테스트")
                .nickname("techstack_testuser")
                .mainType(MemberMainType.DEVELOPER)
                .disclosure(true)
                .used(MemberStatus.ACTIVE)
                .build());

        testGitRepoUrl = gitRepoUrlRepository.saveAndFlush(GitRepoUrl.builder()
                .member(testMember)
                .gitUrl("https://github.com/test/techstack-test-repo")
                .build());

        // Flyway V3 시드 데이터 조회
        backendStack = techstackRepository.findByName(TechName.BACKEND).orElseThrow();
        javaStack = techstackRepository.findByName(TechName.JAVA).orElseThrow();
        springbootStack = techstackRepository.findByName(TechName.SPRINGBOOT).orElseThrow();
    }

    @AfterEach
    void cleanupTestData() throws InterruptedException {
        // 비동기 알림 생성(@Async + AFTER_COMMIT)이 완료될 때까지 대기
        Thread.sleep(500);
        jdbcTemplate.execute("DELETE FROM notification");
        devTechstackRepository.deleteAll();
        devReportRepository.deleteAll();
        gitRepoUrlRepository.deleteById(testGitRepoUrl.getId());
        memberRepository.deleteById(testMember.getId());
    }

    private ReportReqDTO.CreateReportReq createRequest() {
        return ReportReqDTO.CreateReportReq.builder()
                .gitRepoId(testGitRepoUrl.getId())
                .build();
    }

    private FastApiResDto.ReportGenerationSyncRes successResponseWithTechstacks(List<String> techstacks) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode content = objectMapper.createObjectNode();
        content.set("main", objectMapper.createObjectNode().put("summary", "Main report"));
        content.set("detail", objectMapper.createObjectNode().put("summary", "Detail report"));

        return FastApiResDto.ReportGenerationSyncRes.builder()
                .status("SUCCESS")
                .content(content)
                .techstacks(techstacks)
                .build();
    }

    @Nested
    @DisplayName("createReportSync techstacks 저장 테스트")
    class CreateReportSyncTechstacksTest {

        @Test
        @DisplayName("FastAPI 응답의 techstacks가 DevTechstack AUTO로 저장된다")
        void createReportSync_techstacks가_AUTO로_저장된다() {
            // given
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(successResponseWithTechstacks(List.of("JAVA", "SPRINGBOOT")));

            // when
            reportCommandService.createReportSync(testMember.getId(), createRequest());

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);

            // JAVA, SPRINGBOOT + BACKEND(parent) = 3개
            assertThat(devTechstacks).hasSize(3);
            assertThat(devTechstacks).allMatch(dt -> dt.getSource() == TechstackSource.AUTO);

            List<TechName> savedNames = devTechstacks.stream()
                    .map(dt -> dt.getTechstack().getName())
                    .toList();
            assertThat(savedNames).containsExactlyInAnyOrder(TechName.JAVA, TechName.SPRINGBOOT, TechName.BACKEND);
        }

        @Test
        @DisplayName("하위 techstack 추가 시 parent techstack도 함께 추가된다")
        void createReportSync_하위techstack추가시_parent도_추가된다() {
            // given - JAVA만 응답에 포함
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(successResponseWithTechstacks(List.of("JAVA")));

            // when
            reportCommandService.createReportSync(testMember.getId(), createRequest());

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);

            // JAVA + BACKEND(parent) = 2개
            assertThat(devTechstacks).hasSize(2);

            List<TechName> savedNames = devTechstacks.stream()
                    .map(dt -> dt.getTechstack().getName())
                    .toList();
            assertThat(savedNames).containsExactlyInAnyOrder(TechName.JAVA, TechName.BACKEND);
        }

        @Test
        @DisplayName("기존 MANUAL 소스가 AUTO로 업데이트된다")
        void createReportSync_MANUAL소스가_AUTO로_업데이트된다() {
            // given - 기존에 MANUAL로 저장된 DevTechstack
            devTechstackRepository.saveAndFlush(DevTechstack.builder()
                    .member(testMember)
                    .techstack(javaStack)
                    .source(TechstackSource.MANUAL)
                    .build());

            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(successResponseWithTechstacks(List.of("JAVA")));

            // when
            reportCommandService.createReportSync(testMember.getId(), createRequest());

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);

            // JAVA(업데이트) + BACKEND(신규) = 2개
            assertThat(devTechstacks).hasSize(2);
            assertThat(devTechstacks).allMatch(dt -> dt.getSource() == TechstackSource.AUTO);
        }

        @Test
        @DisplayName("null techstacks 응답 시 오류 없이 진행된다")
        void createReportSync_null_techstacks시_오류없이_진행된다() {
            // given
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(successResponseWithTechstacks(null));

            // when
            reportCommandService.createReportSync(testMember.getId(), createRequest());

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);
            assertThat(devTechstacks).isEmpty();

            // 리포트는 정상 저장됨
            List<DevReport> reports = devReportRepository.findAll();
            assertThat(reports).hasSize(2);
            assertThat(reports).allMatch(r -> r.getCompletedAt() != null);
        }

        @Test
        @DisplayName("빈 techstacks 응답 시 오류 없이 진행된다")
        void createReportSync_빈_techstacks시_오류없이_진행된다() {
            // given
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(successResponseWithTechstacks(List.of()));

            // when
            reportCommandService.createReportSync(testMember.getId(), createRequest());

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);
            assertThat(devTechstacks).isEmpty();

            // 리포트는 정상 저장됨
            List<DevReport> reports = devReportRepository.findAll();
            assertThat(reports).hasSize(2);
            assertThat(reports).allMatch(r -> r.getCompletedAt() != null);
        }

        @Test
        @DisplayName("알 수 없는 TechName은 무시된다")
        void createReportSync_알수없는_TechName은_무시된다() {
            // given - JAVA는 유효, UNKNOWN은 무효
            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(successResponseWithTechstacks(List.of("JAVA", "UNKNOWN_TECH")));

            // when
            reportCommandService.createReportSync(testMember.getId(), createRequest());

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);

            // JAVA + BACKEND(parent) = 2개 (UNKNOWN_TECH는 무시됨)
            assertThat(devTechstacks).hasSize(2);

            List<TechName> savedNames = devTechstacks.stream()
                    .map(dt -> dt.getTechstack().getName())
                    .toList();
            assertThat(savedNames).containsExactlyInAnyOrder(TechName.JAVA, TechName.BACKEND);
        }

        @Test
        @DisplayName("이미 AUTO로 저장된 techstack은 그대로 유지된다")
        void createReportSync_기존_AUTO는_유지된다() {
            // given - 기존에 AUTO로 저장된 DevTechstack
            DevTechstack existingAuto = devTechstackRepository.saveAndFlush(DevTechstack.builder()
                    .member(testMember)
                    .techstack(javaStack)
                    .source(TechstackSource.AUTO)
                    .build());

            given(fastApiSyncReportClient.requestReportGenerationSync(
                    any(DevReport.class), any(DevReport.class), anyString(), anyString()))
                    .willReturn(successResponseWithTechstacks(List.of("JAVA")));

            // when
            reportCommandService.createReportSync(testMember.getId(), createRequest());

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);

            // JAVA(기존) + BACKEND(신규) = 2개
            assertThat(devTechstacks).hasSize(2);
            assertThat(devTechstacks).allMatch(dt -> dt.getSource() == TechstackSource.AUTO);
        }
    }

    @Nested
    @DisplayName("processCallback techstacks 저장 테스트")
    class ProcessCallbackTechstacksTest {

        private DevReport mainReport;
        private DevReport detailReport;

        @BeforeEach
        void setUpReports() {
            mainReport = devReportRepository.saveAndFlush(DevReport.builder()
                    .gitRepoUrl(testGitRepoUrl)
                    .reportType(ReportType.MAIN)
                    .build());

            detailReport = devReportRepository.saveAndFlush(DevReport.builder()
                    .gitRepoUrl(testGitRepoUrl)
                    .reportType(ReportType.DETAIL)
                    .build());
        }

        @Test
        @DisplayName("콜백 SUCCESS 시 techstacks가 DevTechstack AUTO로 저장된다")
        void processCallback_techstacks가_AUTO로_저장된다() {
            // given
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode content = objectMapper.createObjectNode();
            content.set("main", objectMapper.createObjectNode().put("summary", "Main"));
            content.set("detail", objectMapper.createObjectNode().put("summary", "Detail"));

            ReportReqDTO.CallbackReq callbackReq = ReportReqDTO.CallbackReq.builder()
                    .mainReportId(mainReport.getId())
                    .detailReportId(detailReport.getId())
                    .status(ReportReqDTO.CallbackStatus.SUCCESS)
                    .content(content)
                    .techstacks(List.of("JAVA", "SPRINGBOOT"))
                    .build();

            // when
            reportCommandService.processCallback(callbackReq);

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);

            // JAVA, SPRINGBOOT + BACKEND(parent) = 3개
            assertThat(devTechstacks).hasSize(3);
            assertThat(devTechstacks).allMatch(dt -> dt.getSource() == TechstackSource.AUTO);
        }

        @Test
        @DisplayName("콜백 SUCCESS 시 하위 techstack의 parent도 함께 추가된다")
        void processCallback_하위techstack추가시_parent도_추가된다() {
            // given
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode content = objectMapper.createObjectNode();
            content.set("main", objectMapper.createObjectNode().put("summary", "Main"));
            content.set("detail", objectMapper.createObjectNode().put("summary", "Detail"));

            ReportReqDTO.CallbackReq callbackReq = ReportReqDTO.CallbackReq.builder()
                    .mainReportId(mainReport.getId())
                    .detailReportId(detailReport.getId())
                    .status(ReportReqDTO.CallbackStatus.SUCCESS)
                    .content(content)
                    .techstacks(List.of("SPRINGBOOT"))
                    .build();

            // when
            reportCommandService.processCallback(callbackReq);

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);

            // SPRINGBOOT + BACKEND(parent) = 2개
            assertThat(devTechstacks).hasSize(2);

            List<TechName> savedNames = devTechstacks.stream()
                    .map(dt -> dt.getTechstack().getName())
                    .toList();
            assertThat(savedNames).containsExactlyInAnyOrder(TechName.SPRINGBOOT, TechName.BACKEND);
        }

        @Test
        @DisplayName("콜백 SUCCESS 시 기존 MANUAL 소스가 AUTO로 업데이트된다")
        void processCallback_MANUAL소스가_AUTO로_업데이트된다() {
            // given - 기존에 MANUAL로 저장된 DevTechstack
            devTechstackRepository.saveAndFlush(DevTechstack.builder()
                    .member(testMember)
                    .techstack(springbootStack)
                    .source(TechstackSource.MANUAL)
                    .build());

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode content = objectMapper.createObjectNode();
            content.set("main", objectMapper.createObjectNode().put("summary", "Main"));
            content.set("detail", objectMapper.createObjectNode().put("summary", "Detail"));

            ReportReqDTO.CallbackReq callbackReq = ReportReqDTO.CallbackReq.builder()
                    .mainReportId(mainReport.getId())
                    .detailReportId(detailReport.getId())
                    .status(ReportReqDTO.CallbackStatus.SUCCESS)
                    .content(content)
                    .techstacks(List.of("SPRINGBOOT"))
                    .build();

            // when
            reportCommandService.processCallback(callbackReq);

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);

            // SPRINGBOOT(업데이트) + BACKEND(신규) = 2개
            assertThat(devTechstacks).hasSize(2);
            assertThat(devTechstacks).allMatch(dt -> dt.getSource() == TechstackSource.AUTO);
        }

        @Test
        @DisplayName("콜백 FAILED 시 techstacks 처리가 생략된다")
        void processCallback_FAILED시_techstacks_처리안됨() {
            // given
            ReportReqDTO.CallbackReq callbackReq = ReportReqDTO.CallbackReq.builder()
                    .mainReportId(mainReport.getId())
                    .detailReportId(detailReport.getId())
                    .status(ReportReqDTO.CallbackStatus.FAILED)
                    .errorMessage("AI 서버 오류")
                    .techstacks(List.of("JAVA", "SPRINGBOOT"))
                    .build();

            // when
            reportCommandService.processCallback(callbackReq);

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);
            assertThat(devTechstacks).isEmpty();
        }

        @Test
        @DisplayName("콜백 SUCCESS 시 null techstacks는 오류 없이 무시된다")
        void processCallback_null_techstacks시_오류없이_진행된다() {
            // given
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode content = objectMapper.createObjectNode();
            content.set("main", objectMapper.createObjectNode().put("summary", "Main"));
            content.set("detail", objectMapper.createObjectNode().put("summary", "Detail"));

            ReportReqDTO.CallbackReq callbackReq = ReportReqDTO.CallbackReq.builder()
                    .mainReportId(mainReport.getId())
                    .detailReportId(detailReport.getId())
                    .status(ReportReqDTO.CallbackStatus.SUCCESS)
                    .content(content)
                    .techstacks(null)
                    .build();

            // when
            reportCommandService.processCallback(callbackReq);

            // then
            List<DevTechstack> devTechstacks = devTechstackRepository.findAllByMemberWithTechstack(testMember);
            assertThat(devTechstacks).isEmpty();

            // 리포트는 정상 완료됨
            DevReport updatedMain = devReportRepository.findById(mainReport.getId()).orElseThrow();
            assertThat(updatedMain.getCompletedAt()).isNotNull();
        }
    }
}
