package com.umc.devine.domain.notice.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NoticeErrorReason implements DomainErrorReason {

    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "NOTICE404_1",
            "해당 공지사항을 찾을 수 없습니다."),
    INVALID_DISPLAY_PERIOD(HttpStatus.BAD_REQUEST,
            "NOTICE400_1",
            "게시 종료 일시는 시작 일시보다 뒤여야 합니다."),
    BLANK_UPDATE_FIELD(HttpStatus.BAD_REQUEST,
            "NOTICE400_2",
            "제목과 내용은 비어 있을 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}