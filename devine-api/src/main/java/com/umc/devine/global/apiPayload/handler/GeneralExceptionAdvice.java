package com.umc.devine.global.apiPayload.handler;

import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;
import com.umc.devine.global.exception.GeneralErrorReason;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {

    // 도메인 예외를 처리
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        DomainErrorReason reason = ex.getReason();
        log.error("DomainException 발생 - code: {}, message: {}", reason.getCode(), reason.getMessage(), ex);

        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, null));
    }

    // 컨트롤러 메서드에서 @Valid 어노테이션을 사용하여 DTO의 유효성 검사를 수행 (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        GeneralErrorReason reason = GeneralErrorReason.VALID_FAIL;
        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, errors));
    }

    // @RequestParam, @PathVariable의 유효성 검사 실패(ConstraintViolationException) 처리 (400)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        ex.getConstraintViolations().forEach(violation -> {
            String fieldPath = violation.getPropertyPath().toString();
            String fieldName = fieldPath.substring(fieldPath.lastIndexOf('.') + 1);

            errors.put(fieldName, violation.getMessage());
        });

        GeneralErrorReason reason = GeneralErrorReason.VALID_FAIL;
        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, errors));
    }

    // JSON 파싱 실패 또는 잘못된 enum 값 처리 (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex
    ) {
        String detail;
        Throwable cause = ex.getCause();

        if (cause instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            Object[] enumConstants = ife.getTargetType().getEnumConstants();
            String validValues = Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            detail = String.format("'%s'은(는) 유효하지 않은 값입니다. 허용 값: [%s]", ife.getValue(), validValues);
        } else {
            detail = "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요.";
        }

        log.error("[HttpMessageNotReadable] {}", detail);

        GeneralErrorReason reason = GeneralErrorReason.BAD_REQUEST;
        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, detail));
    }

    // 그 외의 정의되지 않은 모든 예외 처리 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        log.error("Unhandled Exception 발생 - type: {}, message: {}", ex.getClass().getName(), ex.getMessage(), ex);

        GeneralErrorReason reason = GeneralErrorReason.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, ex.getMessage()));
    }
}
