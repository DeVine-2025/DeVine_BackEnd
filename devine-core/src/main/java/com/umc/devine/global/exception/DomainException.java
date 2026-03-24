package com.umc.devine.global.exception;

import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

    private final DomainErrorReason reason;

    public DomainException(DomainErrorReason reason) {
        super(reason.getMessage());
        this.reason = reason;
    }
}
