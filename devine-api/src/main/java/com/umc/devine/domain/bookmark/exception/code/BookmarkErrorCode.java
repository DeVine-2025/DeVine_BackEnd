package com.umc.devine.domain.bookmark.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum BookmarkErrorCode implements BaseErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, BookmarkErrorReason.NOT_FOUND),
    ALREADY_EXISTS(HttpStatus.BAD_REQUEST, BookmarkErrorReason.ALREADY_EXISTS),
    FORBIDDEN(HttpStatus.FORBIDDEN, BookmarkErrorReason.FORBIDDEN),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, BookmarkErrorReason.INVALID_REQUEST),
    CANNOT_BOOKMARK_SELF(HttpStatus.BAD_REQUEST, BookmarkErrorReason.CANNOT_BOOKMARK_SELF),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
