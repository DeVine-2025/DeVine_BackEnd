package com.umc.devine.domain.payment.enums;

public enum RefundStatus {
    /** 환불 시도 선점 — 외부 취소 호출 진행 중(비종료). */
    IN_PROGRESS,
    /** 환불 완료(종료). */
    COMPLETED,
    /** PG가 요청을 인지하고 거절 — 재시도 허용(종료). */
    FAILED,
    /** 취소 결과 불명(타임아웃/IO/모호한 PG 응답) — 절대 종료 아님, 대사 대상. */
    UNKNOWN
}
