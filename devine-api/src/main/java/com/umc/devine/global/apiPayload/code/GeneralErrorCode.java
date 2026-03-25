package com.umc.devine.global.apiPayload.code;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.GeneralErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum GeneralErrorCode implements BaseErrorCode {

    BAD_REQUEST(HttpStatus.BAD_REQUEST, GeneralErrorReason.BAD_REQUEST),
    VALID_FAIL(HttpStatus.BAD_REQUEST, GeneralErrorReason.VALID_FAIL),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, GeneralErrorReason.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN, GeneralErrorReason.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND, GeneralErrorReason.NOT_FOUND),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, GeneralErrorReason.INTERNAL_SERVER_ERROR),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
