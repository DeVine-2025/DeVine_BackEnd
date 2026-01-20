package com.umc.devine.domain.category.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CategoryErrorCode implements BaseErrorCode {

    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "CATEGORY404_1",
            "존재하지 않는 카테고리입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}