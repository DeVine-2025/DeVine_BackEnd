package com.umc.devine.domain.bookmark.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookmarkSuccessCode implements BaseSuccessCode {

    FOUND(HttpStatus.OK,
            "BOOKMARK200_1",
            "성공적으로 북마크를 조회했습니다."),
    CREATED(HttpStatus.CREATED,
            "BOOKMARK201_1",
            "성공적으로 북마크를 저장했습니다."),
    DELETED(HttpStatus.OK,
            "BOOKMARK200_2",
            "성공적으로 북마크를 삭제했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
