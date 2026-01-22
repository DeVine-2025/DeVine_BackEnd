package com.umc.devine.global.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.security.Principal;

@Getter
@Builder
@AllArgsConstructor
public class ClerkPrincipal implements Principal {

    private final String clerkId;
    private final String email;
    private final String name;
    private final String imageUrl;

    @Override
    public String getName() {
        return clerkId;
    }
}
