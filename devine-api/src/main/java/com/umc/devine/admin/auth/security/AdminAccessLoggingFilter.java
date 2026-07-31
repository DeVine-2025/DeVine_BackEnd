package com.umc.devine.admin.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 인가된 관리자 요청을 감사 로그로 남기는 필터.
 *
 * <p>관리자 체인의 AuthorizationFilter 뒤에 배치되어, 인가를 통과한 요청만 처리한다.
 * (권한 없는 접근은 {@link AdminAccessDeniedHandler}가 별도로 기록한다.)
 * 컴포넌트로 등록하면 서블릿 전역 필터로 자동 등록되므로, 관리자 체인에서만 수동으로 추가한다.
 */
@RequiredArgsConstructor
public class AdminAccessLoggingFilter extends OncePerRequestFilter {

    private final AdminAccessLogger adminAccessLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            String clerkId = AdminAccessLogger.clerkIdOf(SecurityContextHolder.getContext().getAuthentication());
            adminAccessLogger.logAccess(clerkId, request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }
}
