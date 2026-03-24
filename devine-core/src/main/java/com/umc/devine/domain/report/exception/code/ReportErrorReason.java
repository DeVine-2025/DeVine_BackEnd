package com.umc.devine.domain.report.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportErrorReason implements DomainErrorReason {

    REPORT_NOT_FOUND("REPORT404_1", "해당 리포트를 찾을 수 없습니다."),
    GIT_REPO_NOT_FOUND("REPORT404_2", "해당 Git 저장소를 찾을 수 없습니다."),
    UNAUTHORIZED_ACCESS("REPORT403_1", "해당 리포트에 대한 권한이 없습니다."),
    REPORT_ALREADY_EXISTS("REPORT409_1", "해당 Git 저장소에 이미 리포트가 존재합니다."),
    INVALID_JSON_FORMAT("REPORT400_1", "유효하지 않은 JSON 형식입니다."),
    GITHUB_TOKEN_ERROR("REPORT500_1", "GitHub 토큰 조회에 실패했습니다."),
    FASTAPI_REQUEST_FAILED("REPORT500_2", "리포트 생성 요청에 실패했습니다."),
    REPORT_GENERATION_FAILED("REPORT500_3", "리포트 생성에 실패했습니다."),
    EMBEDDING_VECTOR_EMPTY("REPORT400_2", "임베딩 벡터가 비어있습니다."),
    EMBEDDING_INVALID_DIMENSION("REPORT400_3", "임베딩 벡터 차원이 올바르지 않습니다."),
    ;

    private final String code;
    private final String message;
}
