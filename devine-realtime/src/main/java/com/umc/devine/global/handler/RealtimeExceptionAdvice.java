package com.umc.devine.global.handler;

import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class RealtimeExceptionAdvice {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, Object>> handle(DomainException ex) {
        DomainErrorReason reason = ex.getReason();

        log.error("Exception - code: {}, message: {}", reason.getCode(), reason.getMessage(), ex);

        return ResponseEntity.status(reason.getStatus())
                .body(Map.of(
                        "isSuccess", false,
                        "code", reason.getCode(),
                        "message", reason.getMessage()
                ));
    }
}
