package com.umc.devine.domain.image.exception;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.apiPayload.exception.GeneralException;

public class ImageException extends GeneralException {
    public ImageException(BaseErrorCode code) {
        super(code);
    }
}
