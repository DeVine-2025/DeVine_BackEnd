package com.umc.devine.global.exception;

import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

    private final DomainErrorReason reason;

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
