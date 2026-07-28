package com.umc.devine.admin.integration.service.command;

import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;
import com.umc.devine.admin.integration.entity.ExternalIntegrationHealth;
import com.umc.devine.admin.integration.enums.IntegrationStatus;
import com.umc.devine.admin.integration.enums.IntegrationType;
import com.umc.devine.admin.integration.repository.ExternalIntegrationHealthRepository;
import com.umc.devine.infrastructure.health.IntegrationProbe;
import com.umc.devine.infrastructure.health.ProbeResult;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 외부 API를 호출하지 않도록 프로브를 직접 주입해 검증한다.
 */
class AdminIntegrationCommandServiceTest extends IntegrationTestSupport {

    @Autowired
    private IntegrationHealthWriter integrationHealthWriter;

    @Autowired
    private ExternalIntegrationHealthRepository externalIntegrationHealthRepository;

    /** 테스트에서는 병렬성이 아니라 결과가 관심사이므로 호출 스레드에서 바로 실행한다 */
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @AfterEach
    void tearDown() {
        externalIntegrationHealthRepository.deleteAll();
    }

    private AdminIntegrationCommandService serviceWith(IntegrationProbe... probes) {
        return new AdminIntegrationCommandServiceImpl(
                List.of(probes), integrationHealthWriter, DIRECT_EXECUTOR);
    }

    private IntegrationProbe fixedProbe(IntegrationType type, ProbeResult result) {
        return new IntegrationProbe() {
            @Override
            public IntegrationType getType() {
                return type;
            }

            @Override
            public ProbeResult probe() {
                return result;
            }
        };
    }

    private IntegrationProbe throwingProbe(IntegrationType type) {
        return new IntegrationProbe() {
            @Override
            public IntegrationType getType() {
                return type;
            }

            @Override
            public ProbeResult probe() {
                throw new IllegalStateException("프로브 내부 오류");
            }
        };
    }

    @Nested
    @DisplayName("refreshAll")
    class RefreshAll {

        @Test
        @DisplayName("프로브 결과를 저장하고 요약을 반환한다")
        void savesProbeResults() {
            AdminIntegrationCommandService service = serviceWith(
                    fixedProbe(IntegrationType.GITHUB_API, ProbeResult.success(IntegrationStatus.NORMAL, 120)),
                    fixedProbe(IntegrationType.GEMINI, ProbeResult.down(2500, "HTTP 503"))
            );

            AdminIntegrationResDTO.HealthSummaryDTO summary = service.refreshAll();

            assertThat(summary.integrations()).hasSize(2);
            assertThat(summary.checkedAt()).isNotNull();

            List<ExternalIntegrationHealth> saved = externalIntegrationHealthRepository.findAll();
            assertThat(saved).hasSize(2);
            assertThat(saved)
                    .extracting(ExternalIntegrationHealth::getIntegrationType, ExternalIntegrationHealth::getStatus)
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple(IntegrationType.GITHUB_API, IntegrationStatus.NORMAL),
                            org.assertj.core.groups.Tuple.tuple(IntegrationType.GEMINI, IntegrationStatus.DOWN));
        }

        @Test
        @DisplayName("여러 번 실행해도 연동당 한 행만 유지되고 최신 값으로 갱신된다")
        void upsertDoesNotDuplicateRows() {
            AdminIntegrationCommandService normalRun = serviceWith(
                    fixedProbe(IntegrationType.CLERK_API, ProbeResult.success(IntegrationStatus.NORMAL, 100)));
            normalRun.refreshAll();

            AdminIntegrationCommandService downRun = serviceWith(
                    fixedProbe(IntegrationType.CLERK_API, ProbeResult.down(80, "HTTP 401")));
            downRun.refreshAll();

            List<ExternalIntegrationHealth> saved = externalIntegrationHealthRepository.findAll();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getStatus()).isEqualTo(IntegrationStatus.DOWN);
            assertThat(saved.get(0).getErrorMessage()).isEqualTo("HTTP 401");
            assertThat(saved.get(0).getResponseTimeMs()).isEqualTo(80L);
        }

        @Test
        @DisplayName("프로브가 예외를 던져도 UNKNOWN으로 저장되고 나머지 항목은 정상 저장된다")
        void probeExceptionDoesNotHideOtherResults() {
            AdminIntegrationCommandService service = serviceWith(
                    throwingProbe(IntegrationType.PORTONE),
                    fixedProbe(IntegrationType.GITHUB_API, ProbeResult.success(IntegrationStatus.NORMAL, 90))
            );

            AdminIntegrationResDTO.HealthSummaryDTO summary = service.refreshAll();

            assertThat(summary.integrations()).hasSize(2);

            ExternalIntegrationHealth portOne = externalIntegrationHealthRepository
                    .findByIntegrationType(IntegrationType.PORTONE).orElseThrow();
            assertThat(portOne.getStatus()).isEqualTo(IntegrationStatus.UNKNOWN);
            assertThat(portOne.getErrorMessage()).contains("IllegalStateException");

            ExternalIntegrationHealth gitHub = externalIntegrationHealthRepository
                    .findByIntegrationType(IntegrationType.GITHUB_API).orElseThrow();
            assertThat(gitHub.getStatus()).isEqualTo(IntegrationStatus.NORMAL);
        }

        @Test
        @DisplayName("긴 오류 메시지는 error_message 컬럼 길이(500)에 맞게 잘려 저장된다")
        void longErrorMessageIsTruncatedOnSave() {
            AdminIntegrationCommandService service = serviceWith(
                    fixedProbe(IntegrationType.FASTAPI_AI, ProbeResult.unknown("가".repeat(1000))));

            service.refreshAll();

            ExternalIntegrationHealth saved = externalIntegrationHealthRepository
                    .findByIntegrationType(IntegrationType.FASTAPI_AI).orElseThrow();
            assertThat(saved.getErrorMessage()).hasSize(500);
        }

        @Test
        @DisplayName("확인 불가 항목은 응답 시간 없이 저장된다")
        void unknownResultHasNoResponseTime() {
            AdminIntegrationCommandService service = serviceWith(
                    fixedProbe(IntegrationType.OPENAI, ProbeResult.unknown("설정값 없음")));

            service.refreshAll();

            ExternalIntegrationHealth openAi = externalIntegrationHealthRepository
                    .findByIntegrationType(IntegrationType.OPENAI).orElseThrow();
            assertThat(openAi.getStatus()).isEqualTo(IntegrationStatus.UNKNOWN);
            assertThat(openAi.getResponseTimeMs()).isNull();
            assertThat(openAi.getCheckedAt()).isNotNull();
        }
    }
}
