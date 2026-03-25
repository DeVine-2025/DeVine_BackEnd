package com.umc.devine.domain.notification.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, NotificationErrorReason.NOTIFICATION_NOT_FOUND),
    FORBIDDEN(HttpStatus.FORBIDDEN, NotificationErrorReason.FORBIDDEN),
    RECEIVER_NOT_FOUND(HttpStatus.NOT_FOUND, NotificationErrorReason.RECEIVER_NOT_FOUND),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
