package com.umc.devine.domain.image.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
public enum ImageErrorCode implements BaseErrorCode {

    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, ImageErrorReason.IMAGE_NOT_FOUND),
    INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, ImageErrorReason.INVALID_FILE_NAME),
    IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, ImageErrorReason.IMAGE_LIMIT_EXCEEDED),
    UNSUPPORTED_FILE_EXTENSION(HttpStatus.BAD_REQUEST, ImageErrorReason.UNSUPPORTED_FILE_EXTENSION),
    S3_PRESIGN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ImageErrorReason.S3_PRESIGN_FAILED),
    S3_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, ImageErrorReason.S3_DELETE_FAILED),
    IMAGE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, ImageErrorReason.IMAGE_NOT_UPLOADED),
    IMAGE_ACCESS_DENIED(HttpStatus.FORBIDDEN, ImageErrorReason.IMAGE_ACCESS_DENIED),
    IMAGE_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, ImageErrorReason.IMAGE_TYPE_MISMATCH),
    ;

    private final HttpStatus status;
    private final DomainErrorReason reason;

    @Override public HttpStatus getStatus() { return status; }
    @Override public String getCode() { return reason.getCode(); }
    @Override public String getMessage() { return reason.getMessage(); }
    @Override public DomainErrorReason getReason() { return reason; }
}
