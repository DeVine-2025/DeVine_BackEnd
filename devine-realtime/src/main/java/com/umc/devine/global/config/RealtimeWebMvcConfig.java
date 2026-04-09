package com.umc.devine.global.config;

import com.umc.devine.global.security.CurrentMemberArgumentResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 설정 (Realtime 모듈)
 * SSE 비동기 요청 처리 설정
 */
@Configuration
public class RealtimeWebMvcConfig implements WebMvcConfigurer {

    private final AsyncTaskExecutor sseConnectionExecutor;
    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;

    @Value("${sse.timeout}")
    private long sseTimeout;

    public RealtimeWebMvcConfig(
            @Qualifier("sseConnectionExecutor") AsyncTaskExecutor sseConnectionExecutor,
            CurrentMemberArgumentResolver currentMemberArgumentResolver
    ) {
        this.sseConnectionExecutor = sseConnectionExecutor;
        this.currentMemberArgumentResolver = currentMemberArgumentResolver;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(sseTimeout);
        configurer.setTaskExecutor(sseConnectionExecutor);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }
}
