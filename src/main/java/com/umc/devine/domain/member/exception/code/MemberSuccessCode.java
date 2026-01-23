package com.umc.devine.domain.member.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MemberSuccessCode implements BaseSuccessCode {

    FOUND(HttpStatus.OK,
            "MEMBER200_1",
            "성공적으로 사용자를 조회했습니다."),
    UPDATED(HttpStatus.OK,
            "MEMBER200_2",
            "성공적으로 사용자를 업데이트했습니다."),
    FOUND_PROJECT(HttpStatus.OK,
            "MEMBER200_3",
            "성공적으로 사용자의 프로젝트를 조회했습니다."),
    NICKNAME_CHECKED(HttpStatus.OK,
            "MEMBER200_4",
            "닉네임 중복 여부를 확인했습니다."),
    FOUND_REPORT(HttpStatus.OK,
            "MEMBER200_5",
            "성공적으로 리포트를 조회했습니다."),
    FOUND_TECHSTACK(HttpStatus.OK,
            "MEMBER200_6",
            "성공적으로 보유 기술을 조회했습니다."),
    CREATED(HttpStatus.CREATED,
            "MEMBER201_1",
            "성공적으로 사용자를 생성했습니다."),
    CREATED_TECHSTACK(HttpStatus.OK,
            "MEMBER201_2",
            "성공적으로 보유 기술을 생성했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}