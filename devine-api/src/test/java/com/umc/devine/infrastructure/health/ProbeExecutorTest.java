package com.umc.devine.infrastructure.health;

import com.umc.devine.admin.integration.enums.IntegrationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 상태 판정 규칙 단위 테스트. Spring 컨텍스트가 필요 없다.
 */
class ProbeExecutorTest {

    /** 임계값을 넉넉히 잡아 실행 시간 변동이 판정에 영향을 주지 않게 한다 */
    private final ProbeExecutor executor = new ProbeExecutor(10_000);

    @Nested
    @DisplayName("정상 응답")
    class Success {

        @Test
        @DisplayName("임계값보다 빠르면 NORMAL로 판정한다")
        void fastResponseIsNormal() {
            ProbeResult result = executor.execute(() -> { /* 즉시 반환 */ });

            assertThat(result.status()).isEqualTo(IntegrationStatus.NORMAL);
            assertThat(result.responseTimeMs()).isNotNull();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("임계값 이상이면 DELAYED로 판정한다")
        void slowResponseIsDelayed() {
            // 임계값 0ms - 어떤 응답이든 지연으로 판정된다
            ProbeExecutor zeroThreshold = new ProbeExecutor(0);

            ProbeResult result = zeroThreshold.execute(() -> { /* 즉시 반환 */ });

            assertThat(result.status()).isEqualTo(IntegrationStatus.DELAYED);
            assertThat(result.errorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("오류 응답")
    class Failure {

        @Test
        @DisplayName("4xx 응답은 DOWN으로 판정하고 상태 코드를 남긴다")
        void clientErrorIsDown() {
            ProbeResult result = executor.execute(() -> {
                throw HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null);
            });

            assertThat(result.status()).isEqualTo(IntegrationStatus.DOWN);
            assertThat(result.errorMessage()).isEqualTo("HTTP 401");
            assertThat(result.responseTimeMs()).isNotNull();
        }

        @Test
        @DisplayName("5xx 응답은 DOWN으로 판정한다")
        void serverErrorIsDown() {
            ProbeResult result = executor.execute(() -> {
                throw HttpServerErrorException.create(
                        HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", null, null, null);
            });

            assertThat(result.status()).isEqualTo(IntegrationStatus.DOWN);
            assertThat(result.errorMessage()).isEqualTo("HTTP 503");
        }

        @Test
        @DisplayName("타임아웃은 UNKNOWN(확인 불가)으로 판정한다")
        void timeoutIsUnknown() {
            ProbeResult result = executor.execute(() -> {
                throw new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out"));
            });

            assertThat(result.status()).isEqualTo(IntegrationStatus.UNKNOWN);
            assertThat(result.responseTimeMs()).isNull();
            assertThat(result.errorMessage()).contains("ResourceAccessException");
        }

        @Test
        @DisplayName("예상치 못한 예외도 UNKNOWN으로 판정하고 밖으로 던지지 않는다")
        void unexpectedExceptionIsUnknown() {
            ProbeResult result = executor.execute(() -> {
                throw new IllegalStateException("예상치 못한 오류");
            });

            assertThat(result.status()).isEqualTo(IntegrationStatus.UNKNOWN);
            assertThat(result.errorMessage()).contains("IllegalStateException", "예상치 못한 오류");
        }

    }

    @Nested
    @DisplayName("설정값 누락")
    class MissingConfig {

        @Test
        @DisplayName("설정값이 없으면 호출하지 않고 UNKNOWN으로 판정한다")
        void missingConfigIsUnknown() {
            ProbeResult result = executor.missingConfig();

            assertThat(result.status()).isEqualTo(IntegrationStatus.UNKNOWN);
            assertThat(result.responseTimeMs()).isNull();
            assertThat(result.errorMessage()).isEqualTo(ProbeExecutor.MISSING_CONFIG_MESSAGE);
        }
    }
}
