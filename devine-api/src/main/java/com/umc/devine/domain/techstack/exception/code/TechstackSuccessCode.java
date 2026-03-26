package com.umc.devine.domain.techstack.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TechstackSuccessCode implements BaseSuccessCode {

    FOUND(HttpStatus.OK,
            "TECHSTACK200_1",
            "성공적으로 기술스택을 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
