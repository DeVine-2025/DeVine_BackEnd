package com.umc.devine.domain.techstack.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class TechstackException extends DomainException {

    public TechstackException(DomainErrorReason reason) {
        super(reason);
    }
}
