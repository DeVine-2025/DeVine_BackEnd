package com.umc.devine.global.config;

import com.umc.devine.admin.auth.security.AdminAccessDeniedHandler;
import com.umc.devine.admin.auth.security.AdminAccessLogger;
import com.umc.devine.admin.auth.security.AdminAccessLoggingFilter;
import com.umc.devine.admin.auth.security.AdminJwtAuthenticationConverter;
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
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class ApiSecurityConfig {

    private final ClerkJwtAuthenticationConverter clerkJwtAuthenticationConverter;
    private final AdminJwtAuthenticationConverter adminJwtAuthenticationConverter;
    private final AdminAccessDeniedHandler adminAccessDeniedHandler;
    private final AdminAccessLogger adminAccessLogger;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 관리자 전용 체인. /admin/** 경로만 담당하며 ROLE_ADMIN을 요구한다.
     * 일반 유저와 동일한 Clerk JWT로 인증하되, 인가(관리자 판정)만 분리한다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("ADMIN")
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(adminJwtAuthenticationConverter))
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .exceptionHandling(ex -> ex.accessDeniedHandler(adminAccessDeniedHandler))
                // 인가를 통과한 관리자 요청을 감사 로그로 남긴다 (거절은 AdminAccessDeniedHandler가 기록)
                .addFilterAfter(new AdminAccessLoggingFilter(adminAccessLogger), AuthorizationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @Order(2)
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
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/terms/**").permitAll()
                        // 이번 주 추천 프로젝트 조회
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/weekly-best").permitAll()
                        // 개발자 필터링 조회 (비회원 허용)
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/search").permitAll()
                        // 개발자 상세 조회 (비회원 허용)
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/{nickname}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/{nickname}/contributions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/{nickname}/techstacks").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/members/{nickname}").permitAll()
                        // 리포트 상세 조회 (PUBLIC 리포트는 비회원 허용, PRIVATE은 서비스 레이어에서 owner 검증)
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/{gitRepoId}/main").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/{gitRepoId}/detail").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/{nickname}/projects").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/members/{nickname}/git-repos").permitAll()
                        // 프로젝트 필터링 조회 (비회원 허용)
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects").permitAll()
                        // 프로젝트 상세 조회 (비회원 허용)
                        .requestMatchers(HttpMethod.GET, "/api/v1/projects/{projectId}").permitAll()
                        // 공지사항 조회 (비회원 허용)
                        .requestMatchers(HttpMethod.GET, "/api/v1/notices", "/api/v1/notices/*").permitAll()
                        // PortOne 웹훅 (서명 검증으로 인증)
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
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

    /**
     * 점검 모드일 때 일반 요청을 503 점검 안내로 막는 필터.
     *
     * <p>CorsFilter 뒤, Security 체인 앞에 둔다. Security보다 앞이어야 토큰이 없는 요청도
     * 401이 아니라 점검 안내를 받고, CorsFilter보다 뒤여야 503 응답에 CORS 헤더가 붙는다.
     *
     * <p>통과 경로에 {@code /admin/**}이 있어 점검 중에도 관리자는 정상 접근한다.
     * 관리자 판정을 경로로만 하므로 이 필터는 인증/인가 방식을 알 필요가 없다.
     */
    @Bean
    public FilterRegistrationBean<MaintenanceModeFilter> maintenanceModeFilterRegistration(
            MaintenanceModeService maintenanceModeService, ObjectMapper objectMapper) {
        List<String> allowedPaths = List.of(
                // 관리자 콘솔 (점검 모드를 끄려면 반드시 열려 있어야 한다)
                "/admin/**",
                // 인프라 헬스체크
                "/actuator/**",
                // Swagger 문서
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                "/swagger-resources/**", "/api-docs/**", "/webjars/**",
                // 개발용 토큰 발급 페이지 (점검 중 복구 작업에 필요)
                "/dev", "/dev/**",
                // PortOne 웹훅: 유저 요청이 아닌 PG 정산 통보다. PG 재시도 윈도우가 유한해
                // 점검이 길어지면 결제는 됐는데 서버가 모르는 상태가 남으므로 통과시킨다.
                "/api/v1/payments/webhook"
        );

        FilterRegistrationBean<MaintenanceModeFilter> registration = new FilterRegistrationBean<>(
                new MaintenanceModeFilter(maintenanceModeService, objectMapper, allowedPaths));
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }

    /**
     * CorsFilter를 Security 체인 "밖", 체인보다 앞에 등록한다.
     *
     * <p>점검 모드 필터가 Security 앞에서 503을 반환하는데, 체인 안쪽의 CorsFilter는 그 시점에
     * 실행되지 않아 응답에 CORS 헤더가 붙지 않는다. 그러면 브라우저가 본문을 읽기 전에 차단해
     * 사용자는 점검 안내 대신 네트워크 오류만 보게 된다. 이 등록으로 preflight는 여기서 종료되고,
     * 실제 요청은 CORS 헤더가 붙은 채 뒤 필터로 넘어간다.
     *
     * <p>체인 안의 {@code .cors(...)} 설정은 일부러 남겨 둔다. {@code DefaultCorsProcessor}가
     * Access-Control-Allow-Origin이 이미 있으면 건너뛰므로 헤더가 중복되지 않고, 이 필터가
     * 어떤 이유로든 동작하지 않더라도 기존 CORS 동작이 그대로 유지된다.
     *
     * <p>순서는 Security 체인({@code DEFAULT_FILTER_ORDER}) 바로 앞 두 자리를 쓴다.
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
        configuration.setExposedHeaders(List.of("Last-Event-ID")); // SSE를 위해 헤더 노출
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
