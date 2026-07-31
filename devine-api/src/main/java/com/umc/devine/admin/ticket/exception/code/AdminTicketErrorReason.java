package com.umc.devine.admin.ticket.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminTicketErrorReason implements DomainErrorReason {

    REFUND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "ADMINTICKET404_1", "해당 환불 신청을 찾을 수 없습니다."),
    ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "ADMINTICKET400_1", "이미 처리완료된 환불 신청입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
