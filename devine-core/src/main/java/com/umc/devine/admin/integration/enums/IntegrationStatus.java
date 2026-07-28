package com.umc.devine.admin.integration.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 외부 연동 상태 판정 결과.
 * UNKNOWN은 헬스체크 자체가 수행되지 못한 경우(타임아웃, 설정값 누락 등)를 의미한다.
 */
@Getter
@AllArgsConstructor
public enum IntegrationStatus {

    NORMAL("정상"),
    DELAYED("지연"),
    DOWN("장애"),
    UNKNOWN("확인 불가"),
    ;

    private final String label;
}