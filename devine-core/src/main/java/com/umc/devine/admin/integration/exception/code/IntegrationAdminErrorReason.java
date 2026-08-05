package com.umc.devine.admin.integration.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum IntegrationAdminErrorReason implements DomainErrorReason {

    REFRESH_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
            "INTEGRATIONADMIN429_1",
            "재점검 요청이 너무 잦습니다. 잠시 후 다시 시도해주세요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
