package com.umc.devine.domain.project.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProjectErrorReason implements DomainErrorReason {

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "PROJECT404_1", "지금은 이 대상을 볼 수 없어요."),
    FORBIDDEN_PROJECT_ACCESS(HttpStatus.FORBIDDEN, "PROJECT403_2", "해당 프로젝트에 대한 권한이 없습니다."),
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, "PROJECT400_1", "시작일은 오늘 이후여야 합니다."),
    INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST, "PROJECT400_2", "종료 예정일은 시작일 이후여야 합니다."),
    DUPLICATE_PROJECT_PART(HttpStatus.BAD_REQUEST, "PROJECT400_3", "중복된 모집 파트가 존재합니다."),
    PROJECT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "PROJECT400_4", "이미 완료된 프로젝트입니다."),
    INVALID_RECRUITMENT_DEADLINE(HttpStatus.BAD_REQUEST, "PROJECT400_5", "모집 마감일은 오늘 이후여야 합니다."),
    INVALID_LOCATION(HttpStatus.BAD_REQUEST, "PROJECT400_6", "진행 장소를 입력해주세요."),
    INVALID_RECOMMEND_REQUEST(HttpStatus.BAD_REQUEST, "PROJECT400_7", "추천 프로젝트 요청이 올바르지 않습니다."),
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "PROJECT400_8", "페이지 번호는 1 이상이어야 합니다."),
    INVALID_SIZE(HttpStatus.BAD_REQUEST, "PROJECT400_9", "페이지 크기는 1 이상이어야 합니다."),
    EMBEDDING_VECTOR_EMPTY(HttpStatus.BAD_REQUEST, "PROJECT400_13", "임베딩 벡터가 비어있습니다."),
    EMBEDDING_INVALID_DIMENSION(HttpStatus.BAD_REQUEST, "PROJECT400_14", "임베딩 벡터 차원이 올바르지 않습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "PROJECT400_15", "유효하지 않은 프로젝트 상태 전환입니다."),
    HIDDEN_BY_ADMIN(HttpStatus.FORBIDDEN, "PROJECT403_3", "운영 정책에 따라 비공개된 프로젝트예요. 문의: projectdevine2025@gmail.com"),
    LOCKED_BY_COMPLAINT(HttpStatus.CONFLICT, "PROJECT409_4", "신고 검토 중에는 수정·삭제할 수 없어요. 대신 숨길 수 있어요."),
    PENDING_MATCHINGS_EXIST(HttpStatus.CONFLICT, "PROJECT409_1", "대기 중인 지원·제안을 먼저 처리해 주세요."),
    NOT_DELETABLE(HttpStatus.CONFLICT, "PROJECT409_3", "마감한 프로젝트만 삭제할 수 있어요."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
