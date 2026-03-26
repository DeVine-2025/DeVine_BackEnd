package com.umc.devine.domain.bookmark.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class BookmarkException extends DomainException {

    public BookmarkException(DomainErrorReason reason) {
        super(reason);
    }
}
