package com.umc.devine.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.domain.maintenance.service.MaintenanceModeService;
import com.umc.devine.global.filter.MaintenanceModeFilter;
import com.umc.devine.global.security.ClerkJwtAuthenticationConverter;
import com.umc.devine.global.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class RealtimeSecurityConfig {

    private final ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/chat-test.html").permitAll()
                        .requestMatchers("/sse/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(clerkJwtAuthenticationConverter))
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }

    /**
     * 점검 모드일 때 일반 요청을 503 점검 안내로 막는 필터.
     *
     * <p>realtime은 별도 애플리케이션이라 여기에도 등록하지 않으면 점검 중에 채팅/SSE가
     * 계속 살아 있어 차단이 절반만 이뤄진다.
     *
     * <p>한계: WebSocket은 핸드셰이크(HTTP 업그레이드)만 이 필터를 거치므로,
     * 점검 전환 시점에 이미 열려 있던 연결은 끊기지 않는다. 신규 연결만 차단된다.
     */
    @Bean
    public FilterRegistrationBean<MaintenanceModeFilter> maintenanceModeFilterRegistration(
            MaintenanceModeService maintenanceModeService, ObjectMapper objectMapper) {
        List<String> allowedPaths = List.of("/actuator/**");

        FilterRegistrationBean<MaintenanceModeFilter> registration = new FilterRegistrationBean<>(
                new MaintenanceModeFilter(maintenanceModeService, objectMapper, allowedPaths));
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }

    /**
     * CorsFilter를 Security 체인 "밖", 체인보다 앞에 등록한다.
     * 이유와 체인 안 {@code .cors(...)}를 남겨 두는 근거는 devine-api의 ApiSecurityConfig와 동일하다.
     * 사이의 한 자리({@code -1})는 점검 모드 필터 몫이다.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource()));
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 2);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Last-Event-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
