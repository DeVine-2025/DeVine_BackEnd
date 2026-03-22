package com.umc.devine.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Async 메서드 실행용 스레드풀 (core 공통)
 */
@Configuration
@Slf4j
public class CoreExecutorConfig {

    @Value("${executor.async.core-pool-size:5}")
    private int asyncCorePoolSize;

    @Value("${executor.async.max-pool-size:10}")
    private int asyncMaxPoolSize;

    @Value("${executor.async.queue-capacity:100}")
    private int asyncQueueCapacity;

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
}
