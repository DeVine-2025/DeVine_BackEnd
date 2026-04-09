package com.umc.devine.domain.image.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class ImageException extends DomainException {

    public ImageException(DomainErrorReason reason) {
        super(reason);
    }
}
