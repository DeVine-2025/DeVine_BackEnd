package com.umc.devine.domain.coupon.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CouponErrorReason implements DomainErrorReason {

    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND,
            "COUPON404_1",
            "쿠폰을 찾을 수 없습니다."),
    MEMBER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND,
            "COUPON404_2",
            "보유한 쿠폰을 찾을 수 없습니다."),
    COUPON_CODE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "COUPON404_3",
            "존재하지 않는 쿠폰 코드입니다."),
    COUPON_NOT_USABLE(HttpStatus.BAD_REQUEST,
            "COUPON400_1",
            "사용할 수 없는 쿠폰입니다. 만료되었거나 비활성화된 쿠폰입니다."),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST,
            "COUPON400_2",
            "이미 사용된 쿠폰입니다."),
    COUPON_CODE_ALREADY_REDEEMED(HttpStatus.BAD_REQUEST,
            "COUPON400_3",
            "이미 등록된 쿠폰 코드입니다."),
    COUPON_ISSUE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST,
            "COUPON400_4",
            "쿠폰 발급 가능 수량을 초과했습니다."),
    COUPON_NOT_APPLICABLE_PRODUCT(HttpStatus.BAD_REQUEST,
            "COUPON400_5",
            "이 쿠폰은 해당 상품에 적용할 수 없습니다."),
    VALID_PERIOD_INVALID(HttpStatus.BAD_REQUEST,
            "COUPON400_6",
            "유효기간 종료일은 시작일 이후여야 합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
