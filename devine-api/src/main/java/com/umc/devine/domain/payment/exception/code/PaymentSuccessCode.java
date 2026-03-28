package com.umc.devine.domain.payment.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PaymentSuccessCode implements BaseSuccessCode {

    PAYMENT_COMPLETED(HttpStatus.CREATED,
            "PAYMENT201_1",
            "결제가 성공적으로 완료되었습니다."),
    PAYMENT_LIST_FOUND(HttpStatus.OK,
            "PAYMENT200_1",
            "결제 목록을 성공적으로 조회했습니다."),
    CHANNEL_KEY_FOUND(HttpStatus.OK,
            "PAYMENT200_2",
            "채널키를 성공적으로 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
