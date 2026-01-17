package com.umc.devine.domain.project.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MatchingErrorCode implements BaseErrorCode {

    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MATCHING404_1",
            "프로젝트를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MATCHING404_2",
            "회원을 찾을 수 없습니다."),
    MATCHING_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MATCHING404_3",
            "매칭 정보를 찾을 수 없습니다."),
    PROJECT_NOT_RECRUITING(HttpStatus.BAD_REQUEST,
            "MATCHING400_1",
            "모집 중인 프로젝트가 아닙니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT,
            "MATCHING409_1",
            "이미 지원한 프로젝트입니다."),
    ALREADY_PROPOSED(HttpStatus.CONFLICT,
            "MATCHING409_2",
            "이미 제안한 회원입니다."),
    INVALID_MEMBER_TYPE_FOR_APPLY(HttpStatus.FORBIDDEN,
            "MATCHING403_1",
            "개발자만 프로젝트에 지원할 수 있습니다."),
    INVALID_MEMBER_TYPE_FOR_PROPOSE(HttpStatus.FORBIDDEN,
            "MATCHING403_2",
            "PM만 프로젝트를 제안할 수 있습니다."),
    TARGET_NOT_DEVELOPER(HttpStatus.BAD_REQUEST,
            "MATCHING400_2",
            "제안 대상은 개발자여야 합니다."),
    CANNOT_APPLY_OWN_PROJECT(HttpStatus.BAD_REQUEST,
            "MATCHING400_3",
            "본인의 프로젝트에는 지원할 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN,
            "MATCHING403_3",
            "해당 매칭에 대한 권한이 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
