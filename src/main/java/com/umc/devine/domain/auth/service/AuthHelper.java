package com.umc.devine.domain.auth.service;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorCode;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.global.auth.ClerkPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthHelper {

    private final MemberRepository memberRepository;

    public Member getMember(ClerkPrincipal principal) {
        return memberRepository.findByClerkId(principal.getClerkId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_REGISTERED));
    }

    public Long getMemberId(ClerkPrincipal principal) {
        return getMember(principal).getId();
    }

    public boolean isRegistered(ClerkPrincipal principal) {
        return memberRepository.existsByClerkId(principal.getClerkId());
    }
}
