package com.umc.devine.domain.ticket.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TicketSuccessCode implements BaseSuccessCode {

    PRODUCTS_FOUND(HttpStatus.OK,
            "TICKET200_1",
            "티켓 상품 목록을 성공적으로 조회했습니다."),
    CREDIT_FOUND(HttpStatus.OK,
            "TICKET200_2",
            "리포트 생성권 잔여 수량을 성공적으로 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
