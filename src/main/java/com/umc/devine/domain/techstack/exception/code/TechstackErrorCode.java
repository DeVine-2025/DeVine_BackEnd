package com.umc.devine.domain.techstack.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TechstackErrorCode implements BaseErrorCode {

    TECHSTACK_NOT_FOUND(HttpStatus.NOT_FOUND,
            "TECHSTACK404_1",
            "존재하지 않는 기술 스택입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}