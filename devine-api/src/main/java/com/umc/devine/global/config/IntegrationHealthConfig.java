package com.umc.devine.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 외부 연동 헬스체크 전용 설정.
 */
@Configuration
public class IntegrationHealthConfig {

    /**
     * 프로브 병렬 실행용 스레드풀.
     * <p>
     * 공유 asyncTaskExecutor(코어 5)를 쓰지 않는 이유:
     * 프로브가 7개라 2개가 큐에서 대기해 전체 소요 시간이 두 배가 되고,
     * 그동안 리포트/임베딩 @Async 작업이 밀리기 때문이다.
     */
    @Bean("integrationHealthExecutor")
    public Executor integrationHealthExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("health-probe-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
