package com.umc.devine.domain.auth.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED,
            "AUTH401_1",
            "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_2",
            "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_3",
            "만료된 토큰입니다."),
    NOT_REGISTERED(HttpStatus.FORBIDDEN,
            "AUTH403_1",
            "가입되지 않은 사용자입니다."),
    GITHUB_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND,
            "AUTH404_1",
            "GitHub 연동 정보를 찾을 수 없습니다."),
    GITHUB_USER_NOT_FOUND(HttpStatus.NOT_FOUND,
            "AUTH404_2",
            "존재하지 않는 GitHub 사용자입니다."),
    GITHUB_TOKEN_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "AUTH500_1",
            "GitHub 토큰 조회에 실패했습니다."),
    CLERK_API_ERROR(HttpStatus.BAD_GATEWAY,
            "AUTH502_1",
            "Clerk API 호출에 실패했습니다."),
    GITHUB_API_ERROR(HttpStatus.BAD_GATEWAY,
            "AUTH502_2",
            "GitHub API 호출에 실패했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
