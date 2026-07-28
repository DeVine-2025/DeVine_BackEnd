package com.umc.devine.admin.notice.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminNoticeSuccessCode implements BaseSuccessCode {

    NOTICE_CREATED(HttpStatus.CREATED,
            "ADMINNOTICE201_1",
            "공지사항이 성공적으로 등록되었습니다."),
    NOTICE_LIST_FOUND(HttpStatus.OK,
            "ADMINNOTICE200_1",
            "공지사항 목록을 성공적으로 조회했습니다."),
    NOTICE_FOUND(HttpStatus.OK,
            "ADMINNOTICE200_2",
            "공지사항 상세 정보를 성공적으로 조회했습니다."),
    NOTICE_UPDATED(HttpStatus.OK,
            "ADMINNOTICE200_3",
            "공지사항이 성공적으로 수정되었습니다."),
    NOTICE_DELETED(HttpStatus.OK,
            "ADMINNOTICE200_4",
            "공지사항이 성공적으로 삭제되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
