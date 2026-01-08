package com.umc.devine.global.apiPayload.handler;

import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.apiPayload.code.GeneralErrorCode;
import com.umc.devine.global.apiPayload.exception.GeneralException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GeneralExceptionAdvice {

    // 애플리케이션에서 발생하는 커스텀 예외를 처리
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            GeneralException ex
    ) {

        return ResponseEntity.status(ex.getCode().getStatus())
                .body(ApiResponse.onFailure(
                                ex.getCode(),
                                null
                        )
                );
    }

    // 컨트롤러 메서드에서 @Valid 어노테이션을 사용하여 DTO의 유효성 검사를 수행 (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex
    ) {
        // 검사에 실패한 필드와 그에 대한 메시지를 저장하는 Map
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        GeneralErrorCode code = GeneralErrorCode.VALID_FAIL;
        ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(code, errors);

        // 에러 코드, 메시지와 함께 errors를 반환
        return ResponseEntity.status(code.getStatus()).body(errorResponse);
    }

    //@RequestParam, @PathVariable의 유효성 검사 실패(ConstraintViolationException) 처리 (400)
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

        GeneralErrorCode code = GeneralErrorCode.VALID_FAIL; // 400에 해당하는 에러 코드 사용
        ApiResponse<Map<String, String>> errorResponse = ApiResponse.onFailure(code, errors);

        // 400 상태 코드로 반환
        return ResponseEntity.status(code.getStatus()).body(errorResponse);
    }

    // 그 외의 정의되지 않은 모든 예외 처리 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(
            Exception ex
    ) {

        BaseErrorCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(
                                code,
                                ex.getMessage()
                        )
                );
    }
}