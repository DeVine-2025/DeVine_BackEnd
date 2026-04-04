package com.umc.devine.infrastructure.chat.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class ChatPrincipal implements Principal {

    private final String clerkId;
    private final Long memberId;

    @Override
    public String getName() {
        return clerkId;
    }
}
