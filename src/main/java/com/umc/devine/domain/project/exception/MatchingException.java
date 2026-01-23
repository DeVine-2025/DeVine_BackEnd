package com.umc.devine.domain.project.exception;

import com.umc.devine.global.apiPayload.exception.GeneralException;
import com.umc.devine.domain.project.exception.code.MatchingErrorCode;

public class MatchingException extends GeneralException {

    public MatchingException(MatchingErrorCode code) {
        super(code);
    }
}
