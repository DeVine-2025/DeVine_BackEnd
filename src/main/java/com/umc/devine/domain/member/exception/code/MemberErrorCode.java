package com.umc.devine.domain.member.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberErrorCode implements BaseErrorCode {

    INVALID(HttpStatus.BAD_REQUEST,
            "MEMBER400_1",
            "비밀번호가 일치하지 않습니다."),
    NICKNAME_DUPLICATED(HttpStatus.BAD_REQUEST,
            "MEMBER400_2",
            "이미 존재하는 닉네임입니다."),
    EMAIL_DUPLICATED(HttpStatus.BAD_REQUEST,
            "MEMBER400_3",
            "이미 존재하는 이메일입니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED,
            "MEMBER401_1",
            "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,
            "MEMBER403_1",
            "접근 권한이 없습니다."),
    PROFILE_NOT_PUBLIC(HttpStatus.FORBIDDEN,
            "MEMBER403_2",
            "비공개 프로필입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_1",
            "해당 사용자를 찾지 못했습니다."),
    TECHSTACK_ALREADY_EXISTS(HttpStatus.CONFLICT,
            "MEMBER409_1",
            "이미 보유한 기술 스택입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}

