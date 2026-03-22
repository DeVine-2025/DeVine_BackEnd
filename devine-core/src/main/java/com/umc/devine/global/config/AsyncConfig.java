package com.umc.devine.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * @Async 비동기 처리 설정
 * 스레드풀은 ExecutorConfig에서 관리
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    private final Executor asyncTaskExecutor;

    public AsyncConfig(@Qualifier("asyncTaskExecutor") Executor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    /**
     * @Async 기본 실행기 설정
     */
    @Override
    public Executor getAsyncExecutor() {
        return asyncTaskExecutor;
    }

    /**
     * @Async 메서드에서 발생한 예외를 처리하는 핸들러
     * void 반환 타입의 @Async 메서드에서 발생한 예외는 기본적으로 무시되므로
     * 이 핸들러를 통해 예외를 로깅하여 추적 가능하게 함
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    @Slf4j
    private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Async 메서드 예외 발생 - method: {}.{}, params: {}, error: {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    params,
                    ex.getMessage(),
                    ex);
        }
    }
}
