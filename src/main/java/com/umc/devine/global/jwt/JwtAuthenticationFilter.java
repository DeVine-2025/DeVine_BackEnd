package com.umc.devine.global.jwt;

// TODO: UserDetailServiceImpl, TokenService 주석 해제 후 이 필터도 주석 해제
// 도메인 및 에러 처리 체계가 구축되면 아래 코드의 주석을 해제하세요

/*
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailServiceImpl userDetailService;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null) {
                // 블랙리스트 확인 (토큰 검증 전에 먼저 확인)
                if (tokenService.isTokenBlacklisted(token)) {
                    log.warn("블랙리스트에 등록된 토큰입니다 - 토큰: {}", token.substring(0, Math.min(20, token.length())) + "...");
                    // SecurityContext를 명시적으로 비워서 인증 실패 처리
                    SecurityContextHolder.clearContext();
                    // 401 Unauthorized 응답 반환
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"isSuccess\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"로그아웃된 토큰입니다\",\"result\":null}");
                    return;
                }

                // 토큰 유효성 검증
                if (jwtUtil.validateToken(token)) {
                    String loginId = jwtUtil.extractUsername(token);

                    if (loginId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailService.loadUserByUsername(loginId);

                        if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                            // 실제 로그인 검증 구현 부분
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(request)
                            );

                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            log.debug("사용자 '{}' 인증 완료", loginId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return null;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return path.equals("/login") ||
                path.equals("/users/join") ||
                path.equals("/users/check-duplicate") ||
                path.equals("/profile/image") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/facilities") ||
                path.startsWith("/announcements");
    }
}
*/
