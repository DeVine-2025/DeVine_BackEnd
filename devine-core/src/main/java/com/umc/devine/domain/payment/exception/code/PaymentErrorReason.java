package com.umc.devine.domain.payment.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentErrorReason implements DomainErrorReason {

    ALREADY_PROCESSED_PAYMENT(HttpStatus.BAD_REQUEST,
            "PAYMENT400_1",
            "이미 처리된 결제입니다."),
    PAYMENT_NOT_PAID(HttpStatus.BAD_REQUEST,
            "PAYMENT400_2",
            "결제가 완료되지 않았습니다."),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST,
            "PAYMENT400_3",
            "결제 금액이 일치하지 않습니다."),
    PORTONE_API_ERROR(HttpStatus.BAD_GATEWAY,
            "PAYMENT502_1",
            "결제 정보를 조회할 수 없습니다."),
    UNSUPPORTED_PAYMENT_METHOD(HttpStatus.BAD_REQUEST,
            "PAYMENT400_4",
            "지원하지 않는 결제 수단입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "PAYMENT404_1",
            "결제 정보를 찾을 수 없습니다."),
    CREDIT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "PAYMENT500_1",
            "크레딧 지급에 실패했습니다."),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED,
            "PAYMENT401_1",
            "웹훅 서명이 유효하지 않습니다."),
    INVALID_WEBHOOK_BODY(HttpStatus.BAD_REQUEST,
            "PAYMENT400_5",
            "웹훅 요청 본문이 올바르지 않습니다."),
    DUPLICATE_TICKET_PRODUCT(HttpStatus.BAD_REQUEST,
            "PAYMENT400_6",
            "동일한 상품이 중복으로 포함되어 있습니다."),
    PAYMENT_OWNER_MISMATCH(HttpStatus.FORBIDDEN,
            "PAYMENT403_1",
            "결제 소유자가 일치하지 않습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
