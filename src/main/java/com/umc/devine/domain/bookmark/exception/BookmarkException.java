package com.umc.devine.domain.bookmark.exception;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.apiPayload.exception.GeneralException;

public class BookmarkException extends GeneralException {
    public BookmarkException(BaseErrorCode code) {
        super(code);
    }
}
