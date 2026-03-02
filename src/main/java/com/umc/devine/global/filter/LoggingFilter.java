package com.umc.devine.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.umc.devine.global.security.ClerkPrincipal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
@Profile("prod")
public class LoggingFilter extends OncePerRequestFilter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int MAX_BODY_LENGTH = 10_000;

    private static final Set<String> ALLOWED_HEADERS = Set.of(
            "host", "content-type", "content-length", "authorization",
            "x-forwarded-for", "x-real-ip", "x-request-id", "user-agent", "referer", "origin"
    );

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie"
    );

    private final ObjectMapper objectMapper;

    public LoggingFilter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String requestTime = LocalDateTime.now().format(FORMATTER);
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(wrappedRequest, requestTime);
            logResponse(wrappedResponse, duration);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, String requestTime) {
        try {
            Map<String, String> headers = Collections.list(request.getHeaderNames()).stream()
                    .filter(name -> ALLOWED_HEADERS.contains(name.toLowerCase()))
                    .collect(Collectors.toMap(
                            name -> name,
                            name -> SENSITIVE_HEADERS.contains(name.toLowerCase()) ? "***" : request.getHeader(name),
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            String bodyString = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
            Object body = truncateBody(bodyString);

            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("type", "REQUEST");
            logMap.put("timestamp", requestTime);
            logMap.put("userId", resolveUserId());
            logMap.put("method", request.getMethod());
            logMap.put("uri", request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""));
            logMap.put("headers", headers);
            logMap.put("body", body);

            log.info("\n{}", objectMapper.writeValueAsString(logMap));
        } catch (Exception e) {
            log.warn("요청 로깅 실패", e);
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        try {
            Map<String, String> headers = response.getHeaderNames().stream()
                    .collect(Collectors.toMap(
                            name -> name,
                            name -> SENSITIVE_HEADERS.contains(name.toLowerCase()) ? "***" : response.getHeader(name),
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            String bodyString = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
            Object body = truncateBody(bodyString);

            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("type", "RESPONSE");
            logMap.put("status", response.getStatus());
            logMap.put("duration", duration + "ms");
            logMap.put("headers", headers);
            logMap.put("body", body);

            log.info("\n{}", objectMapper.writeValueAsString(logMap));
        } catch (Exception e) {
            log.warn("응답 로깅 실패", e);
        }
    }

    private Object truncateBody(String bodyString) {
        if (bodyString.isEmpty()) {
            return null;
        }
        if (bodyString.length() > MAX_BODY_LENGTH) {
            return bodyString.substring(0, MAX_BODY_LENGTH)
                    + "...(truncated, " + bodyString.length() + " chars)";
        }
        return parseJson(bodyString);
    }

    private String resolveUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof ClerkPrincipal principal) {
                return principal.getClerkId();
            }
        } catch (Exception e) {
            log.trace("Failed to resolve userId from SecurityContext", e);
        }
        return "anonymous";
    }

    private Object parseJson(String raw) {
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception e) {
            return raw;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/sse/")
                || uri.startsWith("/actuator")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs")
                || uri.startsWith("/swagger-resources")
                || uri.startsWith("/webjars")
                || uri.startsWith("/api-docs")
                || uri.equals("/favicon.ico")
                || uri.startsWith("/dev");
    }
}
