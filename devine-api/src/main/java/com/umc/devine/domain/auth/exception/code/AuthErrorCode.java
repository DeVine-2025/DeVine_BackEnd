package com.umc.devine.domain.auth.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, AuthErrorReason.UNAUTHORIZED),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, AuthErrorReason.INVALID_TOKEN),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, AuthErrorReason.EXPIRED_TOKEN),
    NOT_REGISTERED(HttpStatus.FORBIDDEN, AuthErrorReason.NOT_REGISTERED),
    GITHUB_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, AuthErrorReason.GITHUB_TOKEN_NOT_FOUND),
    GITHUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND, AuthErrorReason.GITHUB_USER_NOT_FOUND),
    GITHUB_TOKEN_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, AuthErrorReason.GITHUB_TOKEN_FETCH_FAILED),
    CLERK_API_ERROR(HttpStatus.BAD_GATEWAY, AuthErrorReason.CLERK_API_ERROR),
    GITHUB_API_ERROR(HttpStatus.BAD_GATEWAY, AuthErrorReason.GITHUB_API_ERROR),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
