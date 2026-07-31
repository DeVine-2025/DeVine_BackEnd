package com.umc.devine.admin.ticket.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminTicketSuccessCode implements BaseSuccessCode {

    REFUND_REQUEST_LIST_FOUND(HttpStatus.OK,
            "ADMINTICKET200_1",
            "환불 신청 목록을 성공적으로 조회했습니다."),
    REFUND_REQUEST_PROCESSED(HttpStatus.OK,
            "ADMINTICKET200_2",
            "환불 신청을 처리완료 처리했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
