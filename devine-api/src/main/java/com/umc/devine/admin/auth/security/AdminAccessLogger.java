package com.umc.devine.admin.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * 관리자 접근 감사 로그.
 * 별도 DB 감사 테이블 없이 구조화 로그로만 관리자 접근/거절을 남긴다.
 * (되돌릴 수 없는 관리자 변경 기능이 도입되면 그때 DB 감사 테이블을 검토한다.)
 */
@Slf4j
@Component
public class AdminAccessLogger {

    /** 인가된 관리자 접근 기록 */
    public void logAccess(String clerkId, String method, String path, int status) {
        log.info("[ADMIN-ACCESS] clerkId={} method={} path={} status={}",
                clerkIdOrAnonymous(clerkId), method, path, status);
    }

    /** 권한 없는 접근(403) 기록 */
    public void logDenied(String clerkId, String method, String path, int status) {
        log.warn("[ADMIN-ACCESS-DENIED] clerkId={} method={} path={} status={}",
                clerkIdOrAnonymous(clerkId), method, path, status);
    }

    private String clerkIdOrAnonymous(String clerkId) {
        return clerkId == null ? "anonymous" : clerkId;
    }

    /** SecurityContext의 인증 주체에서 clerkId를 추출한다. */
    public static String clerkIdOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminPrincipal adminPrincipal) {
            return adminPrincipal.getClerkId();
        }
        return authentication.getName();
    }
}