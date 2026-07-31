package com.umc.devine.infrastructure.portone.dto;

import java.time.LocalDateTime;

/**
 * PortOne 결제 취소 시도의 결과. 예외를 상태로 변환해 "이중 환불 방지"의 핵심 분기를 명시화한다.
 *
 * - Succeeded: 취소 성공(또는 이미 취소됨). 종료 처리 가능.
 * - Rejected: PG가 요청을 인지하고 거절. 확정 실패 → FAILED(재시도 허용).
 * - Unknown: 타임아웃/IO/모호한 PG 응답. 서버 상태 알 수 없음 → 절대 FAILED로 종료 금지.
 */
public sealed interface CancelOutcome permits CancelOutcome.Succeeded, CancelOutcome.Rejected, CancelOutcome.Unknown {

    record Succeeded(
            String cancellationId,
            Long amount,
            LocalDateTime cancelledAt,
            boolean alreadyCancelled
    ) implements CancelOutcome {}

    record Rejected(String reason) implements CancelOutcome {}

    record Unknown(String detail) implements CancelOutcome {}
}
