package com.umc.devine.domain.project.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class ProjectException extends DomainException {

    public ProjectException(DomainErrorReason reason) {
        super(reason);
    }
}
