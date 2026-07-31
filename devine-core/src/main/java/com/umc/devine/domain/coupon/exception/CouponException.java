package com.umc.devine.domain.coupon.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class CouponException extends DomainException {
    public CouponException(DomainErrorReason reason) {
        super(reason);
    }
}
