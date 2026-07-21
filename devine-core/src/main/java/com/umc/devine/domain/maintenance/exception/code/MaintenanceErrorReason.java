package com.umc.devine.domain.maintenance.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MaintenanceErrorReason implements DomainErrorReason {

    UNDER_MAINTENANCE(HttpStatus.SERVICE_UNAVAILABLE, "MAINTENANCE503_1", "서비스 점검 중입니다."),
    SETTING_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "MAINTENANCE500_1", "점검 모드 설정을 찾을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
