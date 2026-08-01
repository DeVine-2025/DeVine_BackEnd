package com.umc.devine.admin.integration.service.command;

import com.umc.devine.admin.integration.entity.ExternalIntegrationHealth;
import com.umc.devine.admin.integration.repository.ExternalIntegrationHealthRepository;
import com.umc.devine.infrastructure.health.ProbeOutcome;
import com.umc.devine.infrastructure.health.ProbeResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 점검 결과를 저장하는 트랜잭션 경계.
 * <p>
 * 프로브 실행(HTTP 호출)과 분리된 별도 빈인 이유:
 * 같은 빈 안에서 @Transactional 메서드를 호출하면 프록시를 타지 않아 트랜잭션이 걸리지 않고,
 * 무엇보다 장시간 HTTP 호출 동안 DB 커넥션을 점유해서는 안 되기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class IntegrationHealthWriter {

    /** error_message 컬럼 길이 */
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final ExternalIntegrationHealthRepository externalIntegrationHealthRepository;

    /**
     * 연동별 스냅샷을 upsert한다. integration_type이 UNIQUE라 행이 중복되지 않는다.
     */
    @Transactional
    public List<ExternalIntegrationHealth> saveResults(List<ProbeOutcome> outcomes, LocalDateTime checkedAt) {
        List<ExternalIntegrationHealth> saved = new ArrayList<>();

        for (ProbeOutcome outcome : outcomes) {
            ProbeResult result = outcome.result();

            ExternalIntegrationHealth health = externalIntegrationHealthRepository
                    .findByIntegrationType(outcome.type())
                    .orElseGet(() -> ExternalIntegrationHealth.builder()
                            .integrationType(outcome.type())
                            .status(result.status())
                            .checkedAt(checkedAt)
                            .build());

            health.updateResult(result.status(), result.responseTimeMs(), checkedAt,
                    truncate(result.errorMessage()));
            saved.add(externalIntegrationHealthRepository.save(health));
        }

        return saved;
    }

    /**
     * 오류 메시지 길이 제한은 컬럼을 아는 이 계층에서만 처리한다.
     * 프로브가 만든 메시지 길이를 프로브마다 신경 쓰지 않아도 되게 하기 위함.
     */
    private String truncate(String errorMessage) {
        if (errorMessage == null || errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
