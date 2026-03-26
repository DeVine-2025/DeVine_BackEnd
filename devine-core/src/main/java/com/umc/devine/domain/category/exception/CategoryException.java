package com.umc.devine.domain.category.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class CategoryException extends DomainException {

    public CategoryException(DomainErrorReason reason) {
        super(reason);
    }
}
