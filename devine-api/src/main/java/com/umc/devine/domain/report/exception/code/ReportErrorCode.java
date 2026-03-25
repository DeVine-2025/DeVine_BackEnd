package com.umc.devine.domain.report.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, ReportErrorReason.REPORT_NOT_FOUND),
    GIT_REPO_NOT_FOUND(HttpStatus.NOT_FOUND, ReportErrorReason.GIT_REPO_NOT_FOUND),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, ReportErrorReason.UNAUTHORIZED_ACCESS),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, ReportErrorReason.REPORT_ALREADY_EXISTS),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, ReportErrorReason.INVALID_JSON_FORMAT),
    GITHUB_TOKEN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ReportErrorReason.GITHUB_TOKEN_ERROR),
    FASTAPI_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ReportErrorReason.FASTAPI_REQUEST_FAILED),
    REPORT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ReportErrorReason.REPORT_GENERATION_FAILED),
    EMBEDDING_VECTOR_EMPTY(HttpStatus.BAD_REQUEST, ReportErrorReason.EMBEDDING_VECTOR_EMPTY),
    EMBEDDING_INVALID_DIMENSION(HttpStatus.BAD_REQUEST, ReportErrorReason.EMBEDDING_INVALID_DIMENSION),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
