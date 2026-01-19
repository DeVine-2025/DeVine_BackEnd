package com.umc.devine.domain.techstack.exception;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.apiPayload.exception.GeneralException;

public class TechstackException extends GeneralException {
    public TechstackException(BaseErrorCode code) {
        super(code);
    }
}
