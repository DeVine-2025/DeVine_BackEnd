package com.umc.devine.admin.coupon.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AdminCouponSuccessCode implements BaseSuccessCode {

    COUPON_CREATED(HttpStatus.CREATED,
            "ADMINCOUPON201_1",
            "쿠폰이 성공적으로 생성되었습니다."),
    COUPON_UPDATED(HttpStatus.OK,
            "ADMINCOUPON200_1",
            "쿠폰이 성공적으로 수정되었습니다."),
    COUPON_LIST_FOUND(HttpStatus.OK,
            "ADMINCOUPON200_2",
            "쿠폰 목록을 성공적으로 조회했습니다."),
    COUPON_ISSUED(HttpStatus.CREATED,
            "ADMINCOUPON201_2",
            "쿠폰이 성공적으로 발급되었습니다."),
    COUPON_USAGE_FOUND(HttpStatus.OK,
            "ADMINCOUPON200_3",
            "쿠폰 사용 현황을 성공적으로 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
