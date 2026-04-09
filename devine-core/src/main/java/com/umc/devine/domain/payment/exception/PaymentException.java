package com.umc.devine.domain.payment.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class PaymentException extends DomainException {
    public PaymentException(DomainErrorReason reason) {
        super(reason);
    }
}
