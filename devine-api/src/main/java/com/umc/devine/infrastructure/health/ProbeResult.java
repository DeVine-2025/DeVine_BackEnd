package com.umc.devine.infrastructure.health;

import com.umc.devine.admin.integration.enums.IntegrationStatus;

/**
 * 단일 헬스체크 프로브의 실행 결과.
 *
 * @param status         판정된 상태
 * @param responseTimeMs 응답까지 걸린 시간(ms). 호출 자체를 못 한 경우 null
 * @param errorMessage   실패 사유. 정상/지연인 경우 null
 */
public record ProbeResult(
        IntegrationStatus status,
        Long responseTimeMs,
        String errorMessage
) {

    public static ProbeResult success(IntegrationStatus status, long responseTimeMs) {
        return new ProbeResult(status, responseTimeMs, null);
    }

    public static ProbeResult down(long responseTimeMs, String errorMessage) {
        return new ProbeResult(IntegrationStatus.DOWN, responseTimeMs, errorMessage);
    }

    public static ProbeResult unknown(String errorMessage) {
        return new ProbeResult(IntegrationStatus.UNKNOWN, null, errorMessage);
    }
}
