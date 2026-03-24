package com.umc.devine.global.apiPayload.code;

import com.umc.devine.global.exception.DomainErrorReason;
import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    HttpStatus getStatus();
    String getCode();
    String getMessage();
    DomainErrorReason getReason();
}
