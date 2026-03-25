package com.umc.devine.domain.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ProjectErrorCode implements BaseErrorCode {

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, ProjectErrorReason.PROJECT_NOT_FOUND),
    FORBIDDEN_PROJECT_ACCESS(HttpStatus.FORBIDDEN, ProjectErrorReason.FORBIDDEN_PROJECT_ACCESS),
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, ProjectErrorReason.INVALID_START_DATE),
    INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST, ProjectErrorReason.INVALID_PROJECT_DATE),
    DUPLICATE_PROJECT_PART(HttpStatus.BAD_REQUEST, ProjectErrorReason.DUPLICATE_PROJECT_PART),
    PROJECT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, ProjectErrorReason.PROJECT_ALREADY_COMPLETED),
    INVALID_RECRUITMENT_DEADLINE(HttpStatus.BAD_REQUEST, ProjectErrorReason.INVALID_RECRUITMENT_DEADLINE),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, ProjectErrorReason.INVALID_LOCATION),
    INVALID_RECOMMEND_REQUEST(HttpStatus.BAD_REQUEST, ProjectErrorReason.INVALID_RECOMMEND_REQUEST),
    INVALID_PAGE(HttpStatus.BAD_REQUEST, ProjectErrorReason.INVALID_PAGE),
    INVALID_SIZE(HttpStatus.BAD_REQUEST, ProjectErrorReason.INVALID_SIZE),
    EMBEDDING_VECTOR_EMPTY(HttpStatus.BAD_REQUEST, ProjectErrorReason.EMBEDDING_VECTOR_EMPTY),
    EMBEDDING_INVALID_DIMENSION(HttpStatus.BAD_REQUEST, ProjectErrorReason.EMBEDDING_INVALID_DIMENSION),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, ProjectErrorReason.INVALID_STATUS_TRANSITION),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
