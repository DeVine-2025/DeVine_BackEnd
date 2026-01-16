package com.umc.devine.domain.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ProjectErrorCode implements BaseErrorCode {

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "PROJECT404_1",
            "존재하지 않는 프로젝트입니다."),

    INVALID_PERMISSION(HttpStatus.FORBIDDEN,
            "PROJECT403_1",
            "메인 권한이 PM일 때만 프로젝트를 생성할 수 있습니다."),

    FORBIDDEN_PROJECT_ACCESS(HttpStatus.FORBIDDEN,
            "PROJECT403_2",
            "해당 프로젝트에 대한 권한이 없습니다."),

    INVALID_START_DATE(HttpStatus.BAD_REQUEST,
            "PROJECT400_1",
            "시작일은 오늘 이후여야 합니다."),

    INVALID_PROJECT_DATE(HttpStatus.BAD_REQUEST,
            "PROJECT400_2",
            "종료 예정일은 시작일 이후여야 합니다."),

    DUPLICATE_PROJECT_PART(HttpStatus.BAD_REQUEST,
            "PROJECT400_3",
            "중복된 모집 파트가 존재합니다."),

    PROJECT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST,
            "PROJECT400_4",
            "이미 완료된 프로젝트입니다."),

    INVALID_RECRUITMENT_DEADLINE(HttpStatus.BAD_REQUEST,
            "PROJECT400_5",
            "모집 마감일은 오늘 이후여야 합니다."),

    INVALID_LOCATION(HttpStatus.BAD_REQUEST,
            "PROJECT400_6",
            "진행 장소를 입력해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}