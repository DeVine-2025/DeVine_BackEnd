package com.umc.devine.global.config;

import com.umc.devine.global.security.ClerkPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(ClerkPrincipal.class::isInstance)
                .map(ClerkPrincipal.class::cast)
                .map(ClerkPrincipal::getClerkId);
    }
}
