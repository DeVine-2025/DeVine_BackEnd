package com.umc.devine.admin.auth.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminAuthSuccessCode implements BaseSuccessCode {

    ADMIN_ME_OK(HttpStatus.OK,
            "ADMIN_AUTH200_1",
            "관리자 정보를 성공적으로 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}