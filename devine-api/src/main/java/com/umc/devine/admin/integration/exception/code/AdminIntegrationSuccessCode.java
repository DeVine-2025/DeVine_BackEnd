package com.umc.devine.admin.integration.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminIntegrationSuccessCode implements BaseSuccessCode {

    INTEGRATION_HEALTH_FOUND(HttpStatus.OK,
            "ADMININTEGRATION200_1",
            "외부 연동 상태를 성공적으로 조회했습니다."),
    INTEGRATION_HEALTH_REFRESHED(HttpStatus.OK,
            "ADMININTEGRATION200_2",
            "외부 연동 상태를 성공적으로 재점검했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
