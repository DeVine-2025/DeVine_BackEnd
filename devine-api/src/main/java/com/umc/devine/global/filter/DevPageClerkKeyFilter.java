package com.umc.devine.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * /dev/index.html의 Clerk publishable key 플레이스홀더를 issuer-uri로부터 계산한 값으로 치환한다.
 * publishable key를 프론트에 따로 하드코딩하면 issuer-uri와 다른 Clerk 인스턴스를 가리킬 수 있어 issuer-uri를 단일 소스로 둔다.
 */
@Component
public class DevPageClerkKeyFilter extends OncePerRequestFilter {

    private static final String PLACEHOLDER = "__CLERK_PUBLISHABLE_KEY__";

    private final String clerkPublishableKey;

    public DevPageClerkKeyFilter(@Value("${clerk.issuer-uri}") String clerkIssuerUri) {
        this.clerkPublishableKey = derivePublishableKey(clerkIssuerUri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrappedResponse);

        byte[] originalBody = wrappedResponse.getContentAsByteArray();
        byte[] newBody = new String(originalBody, StandardCharsets.UTF_8)
                .replace(PLACEHOLDER, clerkPublishableKey)
                .getBytes(StandardCharsets.UTF_8);

        response.setContentLength(newBody.length);
        response.getOutputStream().write(newBody);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.equals("/dev") || uri.equals("/dev/index.html"));
    }

    private String derivePublishableKey(String issuerUri) {
        String host = URI.create(issuerUri).getHost();
        String environment = host.endsWith(".clerk.accounts.dev") ? "test" : "live";
        String encodedHost = Base64.getEncoder().encodeToString((host + "$").getBytes(StandardCharsets.UTF_8));
        return "pk_" + environment + "_" + encodedHost;
    }
}
