package com.umc.devine.domain.notice.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NoticeSuccessCode implements BaseSuccessCode {

    NOTICE_LIST_FOUND(HttpStatus.OK,
            "NOTICE200_1",
            "공지사항 목록을 성공적으로 조회했습니다."),
    NOTICE_FOUND(HttpStatus.OK,
            "NOTICE200_2",
            "공지사항을 성공적으로 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
