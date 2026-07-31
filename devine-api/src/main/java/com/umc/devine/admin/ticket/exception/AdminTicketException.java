package com.umc.devine.admin.ticket.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class AdminTicketException extends DomainException {

    public AdminTicketException(DomainErrorReason reason) {
        super(reason);
    }
}
