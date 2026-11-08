package com.umc.devine.domain.complaint.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class ComplaintException extends DomainException {

    public ComplaintException(DomainErrorReason reason) {
        super(reason);
    }
}
