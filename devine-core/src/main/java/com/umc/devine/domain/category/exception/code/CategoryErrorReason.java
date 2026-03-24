package com.umc.devine.domain.category.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CategoryErrorReason implements DomainErrorReason {

    CATEGORY_NOT_FOUND("CATEGORY404_1", "존재하지 않는 카테고리입니다."),
    ;

    private final String code;
    private final String message;
}
