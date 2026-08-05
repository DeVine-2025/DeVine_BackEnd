package com.umc.devine.admin.auth.security;

import com.umc.devine.admin.enums.AdminLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.security.Principal;

/**
 * 관리자 인증 주체.
 * Clerk JWT의 신원 정보(clerkId/email/...)에 더해, admin 테이블에서 확인된 권한 레벨을 담는다.
 * {@code @AuthenticationPrincipal AdminPrincipal}로 관리자 엔드포인트에서 주입받는다.
 */
@Getter
@Builder
@AllArgsConstructor
public class AdminPrincipal implements Principal {

    private final String clerkId;
    private final String email;
    private final String name;
    private final String imageUrl;
    private final AdminLevel level;

    @Override
    public String getName() {
        return clerkId;
    }

    public String getFullName() {
        return name;
    }
}
