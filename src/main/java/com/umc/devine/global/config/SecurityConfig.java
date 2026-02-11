package com.umc.devine.global.config;

import com.umc.devine.global.security.ClerkJwtAuthenticationConverter;
import com.umc.devine.global.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

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
                        // Actuator 헬스체크
                        .requestMatchers("/actuator/**").permitAll()
                        // Swagger
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        // Auth 헬스체크 (인증 불필요)
                        .requestMatchers("/api/v1/auth/health").permitAll()
                        // techstack 필터링 조회
                        .requestMatchers(HttpMethod.GET, "/api/v1/techstacks").permitAll()
                        // 닉네임 중복 체크
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/nickname/**").permitAll()
                        // 이용약관 조회
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/terms").permitAll()
                        // 이번 주 추천 프로젝트 조회
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/weekly-best").permitAll()
                        // 프로젝트 필터링 조회 (비회원 허용)
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects").permitAll()
                        // 프로젝트 상세 조회 (비회원 허용)
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/{projectId}").permitAll()
                        // AI 서버 콜백 (내부 통신) // TODO 마스터키 인증으로 변경 필요
                        .requestMatchers(HttpMethod.POST, "/api/v1/reports/callback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/embeddings/callback/**").permitAll()
                        // 개발용 토큰 발급 페이지
                        .requestMatchers("/dev", "/dev/**").permitAll()
                        // 그 외 모든 요청은 인증 필요
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Last-Event-ID")); // SSE를 위해 헤더 노출
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
