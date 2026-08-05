package com.umc.devine.infrastructure.health;

import com.umc.devine.admin.integration.enums.IntegrationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.TimeUnit;

/**
 * 프로브 공통 실행기. 응답 시간을 측정하고 예외를 상태로 변환한다.
 * 어떤 예외도 밖으로 던지지 않는다 - 한 연동의 실패가 다른 연동의 점검을 막으면 안 되기 때문.
 */
@Slf4j
@Component
public class ProbeExecutor {

    public static final String MISSING_CONFIG_MESSAGE = "설정값 없음";

    private final long warnThresholdMs;

    public ProbeExecutor(@Value("${integration.health.warn-threshold:1000}") long warnThresholdMs) {
        this.warnThresholdMs = warnThresholdMs;
    }

    /**
     * 설정값이 주입되지 않아 호출 자체가 불가능한 경우.
     */
    public ProbeResult missingConfig() {
        return ProbeResult.unknown(MISSING_CONFIG_MESSAGE);
    }

    /**
     * HTTP 호출을 실행하고 소요 시간과 예외에 따라 상태를 판정한다.
     *
     * <pre>
     * 2xx & 임계값 미만  -> NORMAL
     * 2xx & 임계값 이상  -> DELAYED
     * 4xx / 5xx        -> DOWN
     * 타임아웃 / 그 외 예외 -> UNKNOWN
     * </pre>
     */
    public ProbeResult execute(Runnable httpCall) {
        long startedAt = System.nanoTime();
        try {
            httpCall.run();
            long elapsedMs = elapsedMsSince(startedAt);
            IntegrationStatus status = elapsedMs < warnThresholdMs
                    ? IntegrationStatus.NORMAL
                    : IntegrationStatus.DELAYED;
            return ProbeResult.success(status, elapsedMs);

        } catch (HttpStatusCodeException e) {
            // 서버가 응답은 했으나 오류 상태 - 연동 장애로 판정
            return ProbeResult.down(elapsedMsSince(startedAt), "HTTP " + e.getStatusCode().value());

        } catch (ResourceAccessException e) {
            // 연결 실패 / 타임아웃 - 상태를 확인할 수 없음
            return ProbeResult.unknown(describe(e));

        } catch (Exception e) {
            log.warn("[IntegrationHealth] 프로브 실행 중 예상치 못한 오류", e);
            return ProbeResult.unknown(describe(e));
        }
    }

    private long elapsedMsSince(long startedAtNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }

    /**
     * 예외를 사람이 읽을 수 있는 한 줄로 요약한다.
     * 길이 제한은 저장 시점(IntegrationHealthWriter)에서 처리한다.
     */
    public static String describe(Exception e) {
        String message = e.getMessage();
        return "[%s] %s".formatted(e.getClass().getSimpleName(),
                message != null ? message : "상세 메시지 없음");
    }
}
