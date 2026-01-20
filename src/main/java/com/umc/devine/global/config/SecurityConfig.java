package com.umc.devine.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /*
     * TODO: 프로덕션 배포 전 반드시 인증 로직 복원 필요!
     * - JwtAuthenticationFilter 주석 해제 및 적용
     * - CORS 설정 복원
     * - .anyRequest().authenticated() 로 변경
     *
     * 현재 설정은 배포 테스트를 위한 임시 설정
     * 이 상태로 프로덕션 배포 시 모든 API가 인증 없이 노출
     */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Actuator 헬스체크만 허용 (다른 actuator 엔드포인트는 차단)
                        .requestMatchers("/actuator/health").permitAll()
                        // Swagger 허용
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        // TODO: 인증 로직 구현 후 .authenticated()로 변경
                        .anyRequest().permitAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }
}
