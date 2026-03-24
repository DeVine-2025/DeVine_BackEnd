package com.umc.devine.domain.report.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportErrorCode implements BaseErrorCode {

    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_1", "해당 리포트를 찾을 수 없습니다.", ReportErrorReason.REPORT_NOT_FOUND),
    GIT_REPO_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT404_2", "해당 Git 저장소를 찾을 수 없습니다.", ReportErrorReason.GIT_REPO_NOT_FOUND),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "REPORT403_1", "해당 리포트에 대한 권한이 없습니다.", ReportErrorReason.UNAUTHORIZED_ACCESS),
    REPORT_ALREADY_EXISTS(HttpStatus.CONFLICT, "REPORT409_1", "해당 Git 저장소에 이미 리포트가 존재합니다.", ReportErrorReason.REPORT_ALREADY_EXISTS),
    INVALID_JSON_FORMAT(HttpStatus.BAD_REQUEST, "REPORT400_1", "유효하지 않은 JSON 형식입니다.", ReportErrorReason.INVALID_JSON_FORMAT),
    GITHUB_TOKEN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT500_1", "GitHub 토큰 조회에 실패했습니다.", ReportErrorReason.GITHUB_TOKEN_ERROR),
    FASTAPI_REQUEST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT500_2", "리포트 생성 요청에 실패했습니다.", ReportErrorReason.FASTAPI_REQUEST_FAILED),
    REPORT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REPORT500_3", "리포트 생성에 실패했습니다.", ReportErrorReason.REPORT_GENERATION_FAILED),
    EMBEDDING_VECTOR_EMPTY(HttpStatus.BAD_REQUEST, "REPORT400_2", "임베딩 벡터가 비어있습니다.", ReportErrorReason.EMBEDDING_VECTOR_EMPTY),
    EMBEDDING_INVALID_DIMENSION(HttpStatus.BAD_REQUEST, "REPORT400_3", "임베딩 벡터 차원이 올바르지 않습니다.", ReportErrorReason.EMBEDDING_INVALID_DIMENSION),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final DomainErrorReason reason;
}
