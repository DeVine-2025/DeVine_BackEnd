package com.umc.devine.domain.notification.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class NotificationException extends DomainException {

    public NotificationException(DomainErrorReason reason) {
        super(reason);
    }
}
