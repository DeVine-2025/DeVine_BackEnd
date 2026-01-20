package com.umc.devine.domain.project.exception;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.apiPayload.exception.GeneralException;

public class ProjectException extends GeneralException {
    public ProjectException(BaseErrorCode code) {
        super(code);
    }
}
