package com.umc.devine.infrastructure.health;

import com.umc.devine.admin.integration.enums.IntegrationType;

/**
 * 외부 연동 하나의 상태를 점검하는 프로브.
 * 구현체는 @Component로 등록하면 자동으로 점검 대상에 포함된다.
 */
public interface IntegrationProbe {

    IntegrationType getType();

    /**
     * 점검을 수행한다. 어떤 경우에도 예외를 던지지 않고 결과로 변환해 반환해야 한다.
     */
    ProbeResult probe();
}
