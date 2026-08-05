package com.umc.devine.global.scheduler;

import com.umc.devine.admin.integration.service.command.AdminIntegrationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 외부 연동 상태를 주기적으로 갱신한다.
 * 관리자 화면은 이 스케줄러가 저장해 둔 스냅샷을 조회하므로 화면 진입이 빠르다.
 * <p>
 * integration.health.enabled=false로 끌 수 있다.
 * 테스트에서는 반드시 꺼야 한다 - 켜두면 CI가 실제 외부 API를 호출하게 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "integration.health.enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationHealthScheduler {

    private final AdminIntegrationCommandService adminIntegrationCommandService;

    /**
     * fixedDelay를 쓰는 이유: 이전 실행이 끝난 뒤부터 간격을 재므로 점검이 겹치지 않는다.
     * initialDelay는 기동 직후(Flyway 마이그레이션, 커넥션풀 준비)를 피하기 위한 것이다.
     * 개별 프로브가 예외를 삼키므로 정상 흐름에서는 여기까지 예외가 오지 않지만,
     * 스케줄러 스레드가 죽지 않도록 방어적으로 한 번 더 감싼다.
     */
    @Scheduled(
            fixedDelayString = "${integration.health.interval:300000}",
            initialDelayString = "${integration.health.initial-delay:60000}"
    )
    public void checkIntegrations() {
        try {
            adminIntegrationCommandService.refreshAll();
        } catch (Exception e) {
            log.error("[IntegrationHealth] 주기 점검 실패", e);
        }
    }
}
