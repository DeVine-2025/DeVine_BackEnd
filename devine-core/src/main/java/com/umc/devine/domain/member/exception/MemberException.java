package com.umc.devine.domain.member.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class MemberException extends DomainException {

    public MemberException(DomainErrorReason reason) {
        super(reason);
    }
}
