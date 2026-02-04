package com.umc.devine.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 애플리케이션에서 사용하는 모든 스레드풀을 한 곳에서 관리
 *
 * 스레드풀 목록:
 * 1. asyncTaskExecutor   - @Async 메서드 실행용
 * 2. sseConnectionExecutor - Spring MVC SSE 연결 유지용
 * 3. sseDispatchExecutor - Redis -> Client SSE 메시지 전송용
 */
@Configuration
@Slf4j
public class ExecutorConfig {

    // ========== @Async 메서드용 ==========
    @Value("${executor.async.core-pool-size:5}")
    private int asyncCorePoolSize;

    @Value("${executor.async.max-pool-size:10}")
    private int asyncMaxPoolSize;

    @Value("${executor.async.queue-capacity:100}")
    private int asyncQueueCapacity;

    // ========== SSE 연결 유지용 (Spring MVC Async) ==========
    @Value("${executor.sse-connection.core-pool-size:5}")
    private int sseConnectionCorePoolSize;

    @Value("${executor.sse-connection.max-pool-size:10}")
    private int sseConnectionMaxPoolSize;

    @Value("${executor.sse-connection.queue-capacity:25}")
    private int sseConnectionQueueCapacity;

    // ========== SSE 메시지 전송용 (Redis -> Client) ==========
    @Value("${executor.sse-dispatch.core-pool-size:10}")
    private int sseDispatchCorePoolSize;

    @Value("${executor.sse-dispatch.max-pool-size:50}")
    private int sseDispatchMaxPoolSize;

    @Value("${executor.sse-dispatch.queue-capacity:200}")
    private int sseDispatchQueueCapacity;

    /**
     * @Async 메서드 실행용 스레드풀
     * - 이벤트 리스너 비동기 처리
     * - 일반적인 비동기 작업
     */
    @Bean("asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncCorePoolSize);
        executor.setMaxPoolSize(asyncMaxPoolSize);
        executor.setQueueCapacity(asyncQueueCapacity);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("asyncTaskExecutor 초기화 - core: {}, max: {}, queue: {}",
                asyncCorePoolSize, asyncMaxPoolSize, asyncQueueCapacity);
        return executor;
    }

    /**
     * Spring MVC SSE 연결 유지용 스레드풀
     * - HTTP 요청의 비동기 처리 (SSE, DeferredResult)
     * - WebMvcConfigurer.configureAsyncSupport()에서 사용
     */
    @Bean("sseConnectionExecutor")
    public AsyncTaskExecutor sseConnectionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(sseConnectionCorePoolSize);
        executor.setMaxPoolSize(sseConnectionMaxPoolSize);
        executor.setQueueCapacity(sseConnectionQueueCapacity);
        executor.setThreadNamePrefix("sse-conn-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("sseConnectionExecutor 초기화 - core: {}, max: {}, queue: {}",
                sseConnectionCorePoolSize, sseConnectionMaxPoolSize, sseConnectionQueueCapacity);
        return executor;
    }

    /**
     * SSE 메시지 전송용 스레드풀
     * - Redis Pub/Sub에서 수신한 메시지를 클라이언트에게 전송
     * - 한 클라이언트의 네트워크 지연이 다른 클라이언트에 영향주지 않도록 병렬 처리
     */
    @Bean("sseDispatchExecutor")
    public Executor sseDispatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(sseDispatchCorePoolSize);
        executor.setMaxPoolSize(sseDispatchMaxPoolSize);
        executor.setQueueCapacity(sseDispatchQueueCapacity);
        executor.setThreadNamePrefix("sse-dispatch-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("sseDispatchExecutor 초기화 - core: {}, max: {}, queue: {}",
                sseDispatchCorePoolSize, sseDispatchMaxPoolSize, sseDispatchQueueCapacity);
        return executor;
    }
}
