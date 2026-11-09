package com.umc.devine.global.exception;

import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

    private final DomainErrorReason reason;

    /** 실패 응답의 result로 함께 내려줄 값. 없으면 null이다. */
    private final Object result;

    public DomainException(DomainErrorReason reason) {
        this(reason, null);
    }

    public DomainException(DomainErrorReason reason, Object result) {
        super(reason.getMessage());
        this.reason = reason;
        this.result = result;
    }
}
