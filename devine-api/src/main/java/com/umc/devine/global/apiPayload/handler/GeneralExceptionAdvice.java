package com.umc.devine.global.apiPayload.handler;

import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.apiPayload.code.GeneralErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import com.umc.devine.global.exception.DomainException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    // 도메인 예외를 처리 — ErrorCodeRegistry를 통해 HTTP 매핑
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        DomainErrorReason reason = ex.getReason();
        BaseErrorCode errorCode = ErrorCodeRegistry.resolve(reason).orElse(null);

        if (errorCode != null) {
            log.error("DomainException 발생 - code: {}, message: {}", errorCode.getCode(), errorCode.getMessage(), ex);
            return ResponseEntity.status(errorCode.getStatus())
                    .body(ApiResponse.onFailure(errorCode, null));
        }

        // 매핑되지 않은 도메인 예외 — fallback 500
        log.warn("매핑되지 않은 DomainErrorReason: {}", reason.getCode(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
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

        GeneralErrorCode code = GeneralErrorCode.VALID_FAIL;
        ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(code, errors);

        return ResponseEntity.status(code.getStatus()).body(errorResponse);
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

        GeneralErrorCode code = GeneralErrorCode.VALID_FAIL;
        ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(code, errors);

        return ResponseEntity.status(code.getStatus()).body(errorResponse);
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

        GeneralErrorCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, detail));
    }

    // 그 외의 정의되지 않은 모든 예외 처리 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        log.error("Unhandled Exception 발생 - type: {}, message: {}", ex.getClass().getName(), ex.getMessage(), ex);

        GeneralErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(code, ex.getMessage()));
    }
}
