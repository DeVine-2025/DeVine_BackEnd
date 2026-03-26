package com.umc.devine.global.exception;

import org.springframework.http.HttpStatus;

public interface DomainErrorReason {

    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
