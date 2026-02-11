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
    ALREADY_REGISTERED(HttpStatus.CONFLICT,
            "MEMBER409_2",
            "이미 가입된 회원입니다."),
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_2",
            "해당 약관을 찾을 수 없습니다."),
    REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST,
            "MEMBER400_4",
            "필수 약관에 동의해야 합니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_3",
            "해당 카테고리를 찾을 수 없습니다."),
    TECHSTACK_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_4",
            "해당 기술 스택을 찾을 수 없습니다."),
    GITHUB_USERNAME_NOT_FOUND(HttpStatus.NOT_FOUND,
            "MEMBER404_5",
            "GitHub 사용자명이 등록되지 않았습니다."),
    CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST,
            "MEMBER400_5",
            "관심 도메인은 최소 1개 이상 선택해야 합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}

