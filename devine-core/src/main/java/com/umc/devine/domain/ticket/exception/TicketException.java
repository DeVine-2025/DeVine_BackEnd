package com.umc.devine.domain.ticket.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class TicketException extends DomainException {

    public TicketException(DomainErrorReason reason) {
        super(reason);
    }
}
