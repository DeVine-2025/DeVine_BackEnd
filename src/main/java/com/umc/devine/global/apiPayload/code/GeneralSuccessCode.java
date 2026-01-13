package com.umc.devine.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK,
            "COMMON200",
            "요청이 성공하였습니다."),
    CREATED(HttpStatus.CREATED,
            "COMMON201",
            "요청이 성공적으로 생성되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}