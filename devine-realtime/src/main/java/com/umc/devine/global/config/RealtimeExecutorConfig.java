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
 * SSE 전용 스레드풀 (Realtime 모듈)
 */
@Configuration
@Slf4j
public class RealtimeExecutorConfig {

    @Value("${executor.sse-connection.core-pool-size:5}")
    private int sseConnectionCorePoolSize;

    @Value("${executor.sse-connection.max-pool-size:10}")
    private int sseConnectionMaxPoolSize;

    @Value("${executor.sse-connection.queue-capacity:25}")
    private int sseConnectionQueueCapacity;

    @Value("${executor.sse-dispatch.core-pool-size:10}")
    private int sseDispatchCorePoolSize;

    @Value("${executor.sse-dispatch.max-pool-size:50}")
    private int sseDispatchMaxPoolSize;

    @Value("${executor.sse-dispatch.queue-capacity:200}")
    private int sseDispatchQueueCapacity;

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
