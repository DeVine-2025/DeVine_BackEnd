package com.umc.devine.domain.ticket.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TicketErrorReason implements DomainErrorReason {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "TICKET404_1",
            "해당 티켓 상품을 찾을 수 없습니다."),
    PRODUCT_NOT_ACTIVE(HttpStatus.BAD_REQUEST,
            "TICKET400_1",
            "현재 판매 중이지 않은 상품입니다."),
    INSUFFICIENT_CREDITS(HttpStatus.BAD_REQUEST,
            "TICKET400_2",
            "리포트 생성권이 부족합니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST,
            "TICKET400_3",
            "수량은 1개 이상이어야 합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
