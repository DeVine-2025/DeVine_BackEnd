package com.umc.devine.admin.auth.security;

import com.umc.devine.admin.auth.service.AdminAuthorizationService;
import com.umc.devine.admin.entity.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 관리자 전용 SecurityFilterChain(/admin/**)에서만 사용하는 JWT 변환기.
 *
 * <p>Clerk JWT를 검증한 뒤, {@link AdminAuthorizationService}로 관리자 여부를 판정한다.
 * 관리자면 {@code ROLE_ADMIN}을 부여하고 {@link AdminPrincipal}을 주체로 세팅한다.
 * 관리자가 아니면 권한을 부여하지 않아, 인가 단계({@code hasRole("ADMIN")})에서 403으로 거절된다.
 */
@Component
@RequiredArgsConstructor
public class AdminJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final AdminAuthorizationService adminAuthorizationService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String clerkId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String imageUrl = jwt.getClaimAsString("image_url");

        Optional<Admin> admin = adminAuthorizationService.resolveAdmin(clerkId, email);

        AdminPrincipal principal = AdminPrincipal.builder()
                .clerkId(clerkId)
                .email(email)
                .name(name)
                .imageUrl(imageUrl)
                .level(admin.map(Admin::getLevel).orElse(null))
                .build();

        List<GrantedAuthority> authorities = admin.isPresent()
                ? List.of(new SimpleGrantedAuthority(ROLE_ADMIN))
                : Collections.emptyList();

        return new JwtAuthenticationToken(jwt, authorities, principal.getName()) {
            @Override
            public Object getPrincipal() {
                return principal;
            }
        };
    }
}