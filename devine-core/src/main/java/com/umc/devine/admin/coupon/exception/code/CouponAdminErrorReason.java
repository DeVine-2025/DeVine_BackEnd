package com.umc.devine.admin.coupon.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CouponAdminErrorReason implements DomainErrorReason {

    INVALID_ISSUE_REQUEST(HttpStatus.BAD_REQUEST,
            "COUPONADMIN400_1",
            "발급 방식에 필요한 값이 누락되었습니다."),
    CODE_GENERATION_FAILED(HttpStatus.CONFLICT,
            "COUPONADMIN409_1",
            "쿠폰 코드 생성에 반복적으로 실패했습니다. 잠시 후 다시 시도해주세요."),
    DUPLICATE_COUPON_CODE(HttpStatus.CONFLICT,
            "COUPONADMIN409_2",
            "이미 존재하는 쿠폰 코드입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
