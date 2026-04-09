package com.umc.devine.domain.report.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class ReportException extends DomainException {

    public ReportException(DomainErrorReason reason) {
        super(reason);
    }
}
