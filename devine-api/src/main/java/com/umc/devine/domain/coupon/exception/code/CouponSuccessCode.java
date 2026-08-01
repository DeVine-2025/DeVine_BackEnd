package com.umc.devine.domain.coupon.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CouponSuccessCode implements BaseSuccessCode {

    COUPON_REGISTERED(HttpStatus.CREATED,
            "COUPON201_1",
            "쿠폰이 성공적으로 등록되었습니다."),
    MY_COUPON_LIST_FOUND(HttpStatus.OK,
            "COUPON200_1",
            "보유 쿠폰 목록을 성공적으로 조회했습니다."),
    PAYMENT_PREVIEW_CALCULATED(HttpStatus.OK,
            "COUPON200_2",
            "쿠폰 적용 금액이 계산되었습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
