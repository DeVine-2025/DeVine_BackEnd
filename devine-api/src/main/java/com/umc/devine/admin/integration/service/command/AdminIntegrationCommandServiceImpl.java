package com.umc.devine.admin.integration.service.command;

import com.umc.devine.admin.integration.converter.AdminIntegrationConverter;
import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;
import com.umc.devine.admin.integration.entity.ExternalIntegrationHealth;
import com.umc.devine.admin.integration.enums.IntegrationStatus;
import com.umc.devine.infrastructure.health.IntegrationProbe;
import com.umc.devine.infrastructure.health.ProbeExecutor;
import com.umc.devine.infrastructure.health.ProbeOutcome;
import com.umc.devine.infrastructure.health.ProbeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class AdminIntegrationCommandServiceImpl implements AdminIntegrationCommandService {

    private final List<IntegrationProbe> probes;
    private final IntegrationHealthWriter integrationHealthWriter;
    private final Executor integrationHealthExecutor;

    public AdminIntegrationCommandServiceImpl(
            List<IntegrationProbe> probes,
            IntegrationHealthWriter integrationHealthWriter,
            @Qualifier("integrationHealthExecutor") Executor integrationHealthExecutor) {
        this.probes = probes;
        this.integrationHealthWriter = integrationHealthWriter;
        this.integrationHealthExecutor = integrationHealthExecutor;
    }

    /**
     * 이 메서드에는 트랜잭션을 걸지 않는다 - HTTP 호출이 끝날 때까지 DB 커넥션을 붙잡고 있으면 안 된다.
     * 저장만 IntegrationHealthWriter의 짧은 트랜잭션에서 수행한다.
     */
    @Override
    public AdminIntegrationResDTO.HealthSummaryDTO refreshAll() {
        LocalDateTime checkedAt = LocalDateTime.now();

        // 모든 프로브를 먼저 제출해 동시에 시작시킨 뒤 결과를 모은다
        List<CompletableFuture<ProbeOutcome>> futures = probes.stream()
                .map(probe -> CompletableFuture.supplyAsync(
                        () -> new ProbeOutcome(probe.getType(), runSafely(probe)),
                        integrationHealthExecutor))
                .toList();

        List<ProbeOutcome> outcomes = futures.stream()
                .map(CompletableFuture::join)
                .sorted(Comparator.comparing(ProbeOutcome::type))
                .toList();

        logAbnormalResults(outcomes);

        List<ExternalIntegrationHealth> saved = integrationHealthWriter.saveResults(outcomes, checkedAt);
        return AdminIntegrationConverter.toHealthSummaryDTO(saved);
    }

    /**
     * 프로브 구현이 계약을 어기고 예외를 던지더라도 나머지 연동의 점검 결과까지 잃지 않도록 방어한다.
     * (join()이 예외를 전파하면 재점검 전체가 실패한다)
     */
    private ProbeResult runSafely(IntegrationProbe probe) {
        try {
            return probe.probe();
        } catch (Exception e) {
            log.error("[IntegrationHealth] {} 프로브가 예외를 던졌습니다", probe.getType(), e);
            return ProbeResult.unknown(ProbeExecutor.describe(e));
        }
    }

    /**
     * 정상이 아닌 항목만 로그로 남긴다. 매 주기 전체를 찍으면 로그가 의미 없이 커진다.
     */
    private void logAbnormalResults(List<ProbeOutcome> outcomes) {
        outcomes.stream()
                .filter(outcome -> outcome.result().status() != IntegrationStatus.NORMAL)
                .forEach(outcome -> log.warn("[IntegrationHealth] {} 상태 이상 - status: {}, responseTime: {}ms, reason: {}",
                        outcome.type(),
                        outcome.result().status(),
                        outcome.result().responseTimeMs(),
                        outcome.result().errorMessage()));
    }
}
