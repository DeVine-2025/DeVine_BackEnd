package com.umc.devine.domain.auth.exception;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.apiPayload.exception.GeneralException;

public class AuthException extends GeneralException {
    public AuthException(BaseErrorCode code) {
        super(code);
    }
}
