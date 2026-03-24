package com.umc.devine.domain.bookmark.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookmarkErrorCode implements BaseErrorCode {

    NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK404_1", "해당 북마크를 찾을 수 없습니다.", BookmarkErrorReason.NOT_FOUND),
    ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "BOOKMARK400_1", "이미 북마크에 추가되어 있습니다.", BookmarkErrorReason.ALREADY_EXISTS),
    FORBIDDEN(HttpStatus.FORBIDDEN, "BOOKMARK403_1", "북마크에 대한 접근 권한이 없습니다.", BookmarkErrorReason.FORBIDDEN),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "BOOKMARK400_2", "잘못된 북마크 요청입니다. PROJECT 타입은 targetId, DEVELOPER 타입은 targetNickname이 필요합니다.", BookmarkErrorReason.INVALID_REQUEST),
    CANNOT_BOOKMARK_SELF(HttpStatus.BAD_REQUEST, "BOOKMARK400_3", "자기 자신을 북마크할 수 없습니다.", BookmarkErrorReason.CANNOT_BOOKMARK_SELF),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final DomainErrorReason reason;
}
