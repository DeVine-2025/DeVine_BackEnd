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
            "이미 처리된 결제입니다.",
            false),
    PAYMENT_NOT_PAID(HttpStatus.BAD_REQUEST,
            "PAYMENT400_2",
            "결제가 완료되지 않았습니다.",
            false),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST,
            "PAYMENT400_3",
            "결제 금액이 일치하지 않습니다.",
            false),
    PORTONE_API_ERROR(HttpStatus.BAD_GATEWAY,
            "PAYMENT502_1",
            "결제 정보를 조회할 수 없습니다.",
            true),
    UNSUPPORTED_PAYMENT_METHOD(HttpStatus.BAD_REQUEST,
            "PAYMENT400_4",
            "지원하지 않는 결제 수단입니다.",
            false),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "PAYMENT404_1",
            "결제 정보를 찾을 수 없습니다.",
            false),
    CREDIT_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "PAYMENT500_1",
            "크레딧 지급에 실패했습니다.",
            false),
    INVALID_WEBHOOK_SIGNATURE(HttpStatus.UNAUTHORIZED,
            "PAYMENT401_1",
            "웹훅 서명이 유효하지 않습니다.",
            false),
    INVALID_WEBHOOK_BODY(HttpStatus.BAD_REQUEST,
            "PAYMENT400_5",
            "웹훅 요청 본문이 올바르지 않습니다.",
            false),
    DUPLICATE_TICKET_PRODUCT(HttpStatus.BAD_REQUEST,
            "PAYMENT400_6",
            "동일한 상품이 중복으로 포함되어 있습니다.",
            false),
    PAYMENT_OWNER_MISMATCH(HttpStatus.FORBIDDEN,
            "PAYMENT403_1",
            "결제 소유자가 일치하지 않습니다.",
            false),
    FREE_PAYMENT_AMOUNT_NOT_ZERO(HttpStatus.BAD_REQUEST,
            "PAYMENT400_7",
            "쿠폰 적용 후 결제 금액이 0원이 아닙니다. 일반 결제를 이용해주세요.",
            false),
    PAYMENT_ALREADY_REFUNDED(HttpStatus.BAD_REQUEST,
            "PAYMENT400_7",
            "이미 환불되었거나 환불 처리 중인 결제입니다.",
            false),
    REFUND_REJECTED(HttpStatus.BAD_GATEWAY,
            "PAYMENT502_2",
            "결제 취소가 거절되었습니다.",
            false),
    REFUND_RESULT_UNKNOWN(HttpStatus.GATEWAY_TIMEOUT,
            "PAYMENT504_1",
            "결제 취소 결과를 확인할 수 없습니다. 잠시 후 상태가 확정됩니다.",
            false),
    REFUND_SETTLEMENT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "PAYMENT500_2",
            "환불 반영에 실패했습니다. 잠시 후 상태가 확정됩니다.",
            false),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final boolean retryable;
}
