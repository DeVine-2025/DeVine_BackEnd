//package com.umc.devine.global.config;
//
//import com.umc.devine.global.jwt.JwtAuthenticationFilter;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//import java.util.Arrays;
//
//@RequiredArgsConstructor
//public class SecurityConfig {
//    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//
//    @Value("${app.frontend.urls}")
//    private String frontendUrls;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        return http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
//                .csrf(csrf -> csrf.disable())
//                .sessionManagement(session ->
//                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
//                // AuthenticationEntryPoint 설정
//                .exceptionHandling(exception -> exception
//                        .authenticationEntryPoint((request, response, authException) -> {
//                            response.setStatus(401);
//                            response.setContentType("application/json;charset=UTF-8");
//                            // 응답 본문 작성
//                            response.getWriter().write(
//                                    "{" +
//                                            "\"isSuccess\":false," +
//                                            "\"code\":\"UNAUTHORIZED\"," +
//                                            "\"message\":\"인증이 필요합니다\"," +
//                                            "\"result\":null" +
//                                            "}"
//                            );
//                        })
//                )
//                .authorizeHttpRequests(auth -> auth
//                        //Swagger 및 정적 리소스
//                        .requestMatchers(
//                                "/v3/api-docs/**",
//                                "/swagger-ui/**",
//                                "/swagger-ui.html",
//                                "/swagger-config",
//                                "/swagger-resources/**",
//                                "/api-docs/**",
//                                "/webjars/**"
//                        ).permitAll()
//                        .anyRequest().authenticated()
//                )
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//                .formLogin(form -> form.disable())
//                .httpBasic(httpBasic -> httpBasic.disable())
//                .build();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        // 프론트엔드 URL 허용 (쉼표로 구분된 여러 URL 지원)
//        configuration.setAllowedOrigins(Arrays.asList(frontendUrls.split(",")));
//
//        // 모든 HTTP 메소드 허용
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
//
//        // 모든 헤더 허용
//        configuration.setAllowedHeaders(Arrays.asList("*"));
//
//        // 인증 정보 허용 (쿠키, Authorization 헤더 등)
//        configuration.setAllowCredentials(true);
//
//        // Preflight 요청 캐시 시간 (1시간)
//        configuration.setMaxAge(3600L);
//
//        // 노출할 헤더 설정
//        configuration.setExposedHeaders(Arrays.asList("Authorization"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//
//        return source;
//    }
//}
