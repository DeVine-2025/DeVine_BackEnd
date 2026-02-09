package com.umc.devine.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import com.umc.devine.global.auth.CurrentMemberArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC 설정
 * 스레드풀은 ExecutorConfig에서 관리
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;
    private final AsyncTaskExecutor sseConnectionExecutor;

    @Value("${sse.timeout}")
    private long sseTimeout;

    public WebMvcConfig(
            CurrentMemberArgumentResolver currentMemberArgumentResolver,
            @Qualifier("sseConnectionExecutor") AsyncTaskExecutor sseConnectionExecutor
    ) {
        this.currentMemberArgumentResolver = currentMemberArgumentResolver;
        this.sseConnectionExecutor = sseConnectionExecutor;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }
    /**
     * HTTP 비동기 요청 처리 설정 (SSE, DeferredResult 등)
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(sseTimeout);
        configurer.setTaskExecutor(sseConnectionExecutor);
    }

    /**
     * 개발용 토큰 발급 컨트룰러 매핑
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/dev").setViewName("forward:/dev/index.html");
    }

}
