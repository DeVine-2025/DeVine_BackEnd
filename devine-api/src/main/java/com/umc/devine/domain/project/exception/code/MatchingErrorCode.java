package com.umc.devine.domain.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum MatchingErrorCode implements BaseErrorCode {

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, MatchingErrorReason.PROJECT_NOT_FOUND),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, MatchingErrorReason.MEMBER_NOT_FOUND),
    MATCHING_NOT_FOUND(HttpStatus.NOT_FOUND, MatchingErrorReason.MATCHING_NOT_FOUND),
    PROJECT_NOT_RECRUITING(HttpStatus.BAD_REQUEST, MatchingErrorReason.PROJECT_NOT_RECRUITING),
    ALREADY_APPLIED(HttpStatus.CONFLICT, MatchingErrorReason.ALREADY_APPLIED),
    ALREADY_PROPOSED(HttpStatus.CONFLICT, MatchingErrorReason.ALREADY_PROPOSED),
    CANNOT_APPLY_OWN_PROJECT(HttpStatus.BAD_REQUEST, MatchingErrorReason.CANNOT_APPLY_OWN_PROJECT),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, MatchingErrorReason.UNAUTHORIZED_ACCESS),
    NOT_PROJECT_OWNER(HttpStatus.FORBIDDEN, MatchingErrorReason.NOT_PROJECT_OWNER),
    ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, MatchingErrorReason.ALREADY_CANCELLED),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, MatchingErrorReason.INVALID_STATUS_TRANSITION),
    NOT_TARGET_MEMBER(HttpStatus.FORBIDDEN, MatchingErrorReason.NOT_TARGET_MEMBER),
    APPLY_MATCHING_NOT_FOUND(HttpStatus.NOT_FOUND, MatchingErrorReason.APPLY_MATCHING_NOT_FOUND),
    PROPOSE_MATCHING_NOT_FOUND(HttpStatus.NOT_FOUND, MatchingErrorReason.PROPOSE_MATCHING_NOT_FOUND),
    PART_REQUIRED(HttpStatus.BAD_REQUEST, MatchingErrorReason.PART_REQUIRED),
    INVALID_PART(HttpStatus.BAD_REQUEST, MatchingErrorReason.INVALID_PART),
    PART_ALREADY_FULFILLED(HttpStatus.CONFLICT, MatchingErrorReason.PART_ALREADY_FULFILLED),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
