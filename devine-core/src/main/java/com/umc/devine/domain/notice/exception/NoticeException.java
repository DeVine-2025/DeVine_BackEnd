package com.umc.devine.domain.notice.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class NoticeException extends DomainException {
    public NoticeException(DomainErrorReason reason) {
        super(reason);
    }
}