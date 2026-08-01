package com.umc.devine.admin.member.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class MemberAdminException extends DomainException {

    public MemberAdminException(DomainErrorReason reason) {
        super(reason);
    }
}
