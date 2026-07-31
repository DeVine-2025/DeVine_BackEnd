package com.umc.devine.global.exception;

import org.springframework.http.HttpStatus;

public interface DomainErrorReason {

    HttpStatus getStatus();
    String getCode();
    String getMessage();

    /**
     * 일시적 오류라 동일 요청을 재시도하면 성공할 수 있는지 여부.
     * 웹훅처럼 호출 측이 실패 시 재시도해주는 경로에서, 재시도해도 소용없는 비즈니스 오류와 구분하는 데 사용한다.
     */
    default boolean isRetryable() {
        return false;
    }
}
