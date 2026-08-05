package com.umc.devine.infrastructure.health;

import com.umc.devine.admin.integration.enums.IntegrationType;

/**
 * 어떤 연동을 점검했고 결과가 무엇인지 묶은 값.
 */
public record ProbeOutcome(
        IntegrationType type,
        ProbeResult result
) {}
