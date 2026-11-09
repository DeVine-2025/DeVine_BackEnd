package com.umc.devine.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${fastapi.report.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    // 비동기 FastAPI 커넥션
    @Value("${fastapi.report.connect-timeout:60000}")
    private int connectTimeout;

    @Value("${fastapi.report.read-timeout:30000}")
    private int readTimeout;

    // 동기 FastAPI 커넥션 (평균처리 2분)
    @Value("${fastapi.report.sync-read-timeout:180000}")
    private int syncReadTimeout;

    // 외부 연동 헬스체크 커넥션 (짧은 타임아웃 필수)
    @Value("${integration.health.connect-timeout:2000}")
    private int healthConnectTimeout;

    @Value("${integration.health.read-timeout:3000}")
    private int healthReadTimeout;

    @Value("${external-api.connect-timeout:3000}")
    private int externalApiConnectTimeout;

    @Value("${external-api.read-timeout:10000}")
    private int externalApiReadTimeout;

    @Bean
    @Primary
    public RestClient restClient() {
        return RestClient.builder()
                .build();
    }

    @Bean
    public RestClient externalApiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(externalApiConnectTimeout))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(externalApiReadTimeout));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RestClient fastApiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RestClient fastApiSyncRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(syncReadTimeout);

        return RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * 외부 연동 헬스체크 전용 클라이언트.
     * @Primary restClient()는 타임아웃이 없어 응답하지 않는 외부 API에 무한정 매달리므로
     * 헬스체크에는 반드시 이 빈을 사용한다.
     */
    @Bean
    public RestClient healthCheckRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(healthConnectTimeout);
        factory.setReadTimeout(healthReadTimeout);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
