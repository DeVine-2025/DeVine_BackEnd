package com.umc.devine.global.handler;

import com.umc.devine.domain.auth.exception.code.AuthErrorReason;
import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class RealtimeExceptionAdvice {

    private static final Map<DomainErrorReason, HttpStatus> STATUS_MAP = Map.of(
            AuthErrorReason.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
            AuthErrorReason.NOT_REGISTERED, HttpStatus.FORBIDDEN
    );

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handle(DomainException ex) {
        DomainErrorReason reason = ex.getReason();
        HttpStatus status = STATUS_MAP.getOrDefault(reason, HttpStatus.INTERNAL_SERVER_ERROR);

        log.error("Exception - code: {}, message: {}", reason.getCode(), reason.getMessage(), ex);

        return ResponseEntity.status(status)
                .body(Map.of(
                        "isSuccess", false,
                        "code", reason.getCode(),
                        "message", reason.getMessage()
                ));
    }
}
