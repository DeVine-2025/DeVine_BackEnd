package com.umc.devine.domain.notification.exception;

import com.umc.devine.domain.notification.exception.code.NotificationErrorCode;
import com.umc.devine.global.apiPayload.exception.GeneralException;

public class NotificationException extends GeneralException {

    public NotificationException(NotificationErrorCode code) {
        super(code);
    }
}
