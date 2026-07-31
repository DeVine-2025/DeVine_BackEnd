package com.umc.devine.admin.coupon.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class CouponAdminException extends DomainException {
    public CouponAdminException(DomainErrorReason reason) {
        super(reason);
    }
}
