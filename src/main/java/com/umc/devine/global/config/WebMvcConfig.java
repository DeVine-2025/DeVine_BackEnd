package com.umc.devine.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import com.umc.devine.global.auth.CurrentMemberArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @Value("${sse.timeout}")
    private long sseTimeout;

    @Value("${sse.executor.core-pool-size:5}")
    private int corePoolSize;

    @Value("${sse.executor.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${sse.executor.queue-capacity:25}")
    private int queueCapacity;

    @Value("${cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}")
    private List<String> allowedOrigins;

    /**
     * CurrentMember 어노테이션 리졸버 등록
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }

    /**
     * 비동기 요청 처리를 위한 설정을 구성(타임아웃, 비동기 SSE 스레드풀)
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(sseTimeout);
        configurer.setTaskExecutor(sseTaskExecutor());
    }

    /**
     * 비동기 스레드풀
     */
    @Bean
    public AsyncTaskExecutor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize); // 기본 스레드 수
        executor.setMaxPoolSize(maxPoolSize); // 최대 스레드 수
        executor.setQueueCapacity(queueCapacity); // 큐 용량
        executor.setThreadNamePrefix("sse-executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
