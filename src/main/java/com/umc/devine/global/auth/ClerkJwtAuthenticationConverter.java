package com.umc.devine.global.auth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ClerkJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String clerkId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String imageUrl = jwt.getClaimAsString("image_url");

        ClerkPrincipal principal = ClerkPrincipal.builder()
                .clerkId(clerkId)
                .email(email)
                .name(name)
                .imageUrl(imageUrl)
                .build();

        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );

        return new JwtAuthenticationToken(jwt, authorities, principal.getName()) {
            @Override
            public Object getPrincipal() {
                return principal;
            }
        };
    }
}
