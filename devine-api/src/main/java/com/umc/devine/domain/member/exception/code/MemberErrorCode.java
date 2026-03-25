package com.umc.devine.domain.member.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    INVALID(HttpStatus.BAD_REQUEST, MemberErrorReason.INVALID),
    NICKNAME_DUPLICATED(HttpStatus.BAD_REQUEST, MemberErrorReason.NICKNAME_DUPLICATED),
    EMAIL_DUPLICATED(HttpStatus.BAD_REQUEST, MemberErrorReason.EMAIL_DUPLICATED),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, MemberErrorReason.LOGIN_REQUIRED),
    FORBIDDEN(HttpStatus.FORBIDDEN, MemberErrorReason.FORBIDDEN),
    PROFILE_NOT_PUBLIC(HttpStatus.FORBIDDEN, MemberErrorReason.PROFILE_NOT_PUBLIC),
    NOT_FOUND(HttpStatus.NOT_FOUND, MemberErrorReason.NOT_FOUND),
    TECHSTACK_ALREADY_EXISTS(HttpStatus.CONFLICT, MemberErrorReason.TECHSTACK_ALREADY_EXISTS),
    ALREADY_REGISTERED(HttpStatus.CONFLICT, MemberErrorReason.ALREADY_REGISTERED),
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, MemberErrorReason.TERMS_NOT_FOUND),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, MemberErrorReason.REQUIRED_TERMS_NOT_AGREED),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, MemberErrorReason.CATEGORY_NOT_FOUND),
    TECHSTACK_NOT_FOUND(HttpStatus.NOT_FOUND, MemberErrorReason.TECHSTACK_NOT_FOUND),
    GITHUB_USERNAME_NOT_FOUND(HttpStatus.NOT_FOUND, MemberErrorReason.GITHUB_USERNAME_NOT_FOUND),
    CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, MemberErrorReason.CATEGORY_REQUIRED),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
