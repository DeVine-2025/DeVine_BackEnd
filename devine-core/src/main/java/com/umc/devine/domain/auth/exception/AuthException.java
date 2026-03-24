package com.umc.devine.domain.auth.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class AuthException extends DomainException {

    public AuthException(DomainErrorReason reason) {
        super(reason);
    }
}
