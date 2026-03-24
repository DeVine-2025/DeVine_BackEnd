package com.umc.devine.domain.project.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class MatchingException extends DomainException {

    public MatchingException(DomainErrorReason reason) {
        super(reason);
    }
}
