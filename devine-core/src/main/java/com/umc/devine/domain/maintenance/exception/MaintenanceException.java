package com.umc.devine.domain.maintenance.exception;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;

public class MaintenanceException extends DomainException {

    public MaintenanceException(DomainErrorReason errorReason) {
        super(errorReason);
    }
}
