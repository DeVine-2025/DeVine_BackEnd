package com.umc.devine.admin.complaint.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ComplaintSuccessCode implements BaseSuccessCode {

    COMPLAINT_LIST_FOUND(HttpStatus.OK, "COMPLAINT200_1", "성공적으로 신고 목록을 조회했습니다."),
    COMPLAINT_DETAIL_FOUND(HttpStatus.OK, "COMPLAINT200_2", "성공적으로 신고 상세를 조회했습니다."),
    STATUS_UPDATED(HttpStatus.OK, "COMPLAINT200_3", "성공적으로 신고 처리 상태를 변경했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
