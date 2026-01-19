package com.umc.devine.domain.bookmark.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookmarkErrorCode implements BaseErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND,
            "BOOKMARK404_1",
            "해당 북마크를 찾을 수 없습니다."),
    ALREADY_EXISTS(HttpStatus.BAD_REQUEST,
            "BOOKMARK400_1",
            "이미 북마크에 추가되어 있습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN,
            "BOOKMARK403_1",
            "북마크에 대한 접근 권한이 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
