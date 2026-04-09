package com.umc.devine.domain.auth.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthSuccessCode implements BaseSuccessCode {

    HEALTH_OK(HttpStatus.OK,
            "AUTH200_1",
            "서버가 정상적으로 동작 중입니다."),
    TOKEN_VALID(HttpStatus.OK,
            "AUTH200_2",
            "사용자 정보를 성공적으로 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
