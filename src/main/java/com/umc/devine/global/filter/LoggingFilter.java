package com.umc.devine.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
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
                    .filter(name -> !name.equalsIgnoreCase("cookie"))
                    .collect(Collectors.toMap(
                            name -> name,
                            request::getHeader,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            String bodyString = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
            Object body = bodyString.isEmpty() ? null : parseJson(bodyString);

            Map<String, Object> logMap = new LinkedHashMap<>();
            logMap.put("type", "REQUEST");
            logMap.put("timestamp", requestTime);
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
                            response::getHeader,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            String bodyString = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
            Object body = bodyString.isEmpty() ? null : parseJson(bodyString);

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

    private Object parseJson(String raw) {
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception e) {
            return raw;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().contains("dev");
    }
}
