package com.umc.devine.admin.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.devine.global.exception.GeneralErrorReason;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 관리자 체인에서 권한 없는 접근(403)을 처리한다.
 * 접근을 감사 로그로 남기고, 관리자 존재 여부를 노출하지 않는 일반 메시지를 응답한다.
 */
@Component
@RequiredArgsConstructor
public class AdminAccessDeniedHandler implements AccessDeniedHandler {

    private final AdminAccessLogger adminAccessLogger;
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String clerkId = AdminAccessLogger.clerkIdOf(SecurityContextHolder.getContext().getAuthentication());
        adminAccessLogger.logDenied(clerkId, request.getMethod(), request.getRequestURI(),
                HttpStatus.FORBIDDEN.value());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = Map.of(
                "isSuccess", false,
                "code", GeneralErrorReason.FORBIDDEN.getCode(),
                "message", GeneralErrorReason.FORBIDDEN.getMessage()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}