package com.umc.devine.infrastructure.fastapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FastApiClientConfig {

    @Value("${fastapi.report.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    @Value("${fastapi.report.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${fastapi.report.read-timeout:30000}")
    private int readTimeout;

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
}
