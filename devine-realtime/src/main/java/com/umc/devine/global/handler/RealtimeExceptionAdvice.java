package com.umc.devine.global.handler;

import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;
import com.umc.devine.global.exception.GeneralErrorReason;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class RealtimeExceptionAdvice {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        DomainErrorReason reason = ex.getReason();
        log.error("DomainException - code: {}, message: {}", reason.getCode(), reason.getMessage(), ex);
        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        GeneralErrorReason reason = GeneralErrorReason.VALID_FAIL;
        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        log.error("Unhandled Exception - type: {}, message: {}", ex.getClass().getName(), ex.getMessage(), ex);
        GeneralErrorReason reason = GeneralErrorReason.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, ex.getMessage()));
    }
}
