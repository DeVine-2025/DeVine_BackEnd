package com.umc.devine.domain.techstack.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TechstackErrorReason implements DomainErrorReason {

    NOT_FOUND("TECHSTACK404_1", "존재하지 않는 기술 스택입니다."),
    ;

    private final String code;
    private final String message;
}
