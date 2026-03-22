package com.umc.devine.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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

    @Bean
    @Primary
    public RestClient restClient() {
        return RestClient.builder()
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
}
