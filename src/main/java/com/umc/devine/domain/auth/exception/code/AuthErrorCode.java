package com.umc.devine.domain.auth.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,
            "AUTH401_1",
            "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_2",
            "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_3",
            "만료된 토큰입니다."),
    NOT_REGISTERED(HttpStatus.FORBIDDEN,
            "AUTH403_1",
            "가입되지 않은 사용자입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
