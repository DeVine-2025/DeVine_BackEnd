package com.umc.devine.domain.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProjectErrorCode implements BaseErrorCode {

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT404_1", "존재하지 않는 프로젝트입니다.", ProjectErrorReason.PROJECT_NOT_FOUND),
    FORBIDDEN_PROJECT_ACCESS(HttpStatus.FORBIDDEN, "PROJECT403_2", "해당 프로젝트에 대한 권한이 없습니다.", ProjectErrorReason.FORBIDDEN_PROJECT_ACCESS),
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, "PROJECT400_1", "시작일은 오늘 이후여야 합니다.", ProjectErrorReason.INVALID_START_DATE),
    INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST, "PROJECT400_2", "종료 예정일은 시작일 이후여야 합니다.", ProjectErrorReason.INVALID_PROJECT_DATE),
    DUPLICATE_PROJECT_PART(HttpStatus.BAD_REQUEST, "PROJECT400_3", "중복된 모집 파트가 존재합니다.", ProjectErrorReason.DUPLICATE_PROJECT_PART),
    PROJECT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PROJECT400_4", "이미 완료된 프로젝트입니다.", ProjectErrorReason.PROJECT_ALREADY_COMPLETED),
    INVALID_RECRUITMENT_DEADLINE(HttpStatus.BAD_REQUEST, "PROJECT400_5", "모집 마감일은 오늘 이후여야 합니다.", ProjectErrorReason.INVALID_RECRUITMENT_DEADLINE),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "PROJECT400_6", "진행 장소를 입력해주세요.", ProjectErrorReason.INVALID_LOCATION),
    INVALID_RECOMMEND_REQUEST(HttpStatus.BAD_REQUEST, "PROJECT400_7", "추천 프로젝트 요청이 올바르지 않습니다.", ProjectErrorReason.INVALID_RECOMMEND_REQUEST),
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "PROJECT400_8", "페이지 번호는 1 이상이어야 합니다.", ProjectErrorReason.INVALID_PAGE),
    INVALID_SIZE(HttpStatus.BAD_REQUEST, "PROJECT400_9", "페이지 크기는 1 이상이어야 합니다.", ProjectErrorReason.INVALID_SIZE),
    EMBEDDING_VECTOR_EMPTY(HttpStatus.BAD_REQUEST, "PROJECT400_13", "임베딩 벡터가 비어있습니다.", ProjectErrorReason.EMBEDDING_VECTOR_EMPTY),
    EMBEDDING_INVALID_DIMENSION(HttpStatus.BAD_REQUEST, "PROJECT400_14", "임베딩 벡터 차원이 올바르지 않습니다.", ProjectErrorReason.EMBEDDING_INVALID_DIMENSION),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "PROJECT400_15", "유효하지 않은 프로젝트 상태 전환입니다.", ProjectErrorReason.INVALID_STATUS_TRANSITION),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final DomainErrorReason reason;
}
