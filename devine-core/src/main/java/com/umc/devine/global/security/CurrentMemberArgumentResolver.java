package com.umc.devine.global.security;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorReason;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberStatus;
import com.umc.devine.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CurrentMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private static final List<MemberStatus> SANCTIONED_INCLUDED_STATUSES =
            List.of(MemberStatus.ACTIVE, MemberStatus.SUSPENDED, MemberStatus.PENDING_WITHDRAWAL);

    private final MemberRepository memberRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentMember.class)
                && parameter.getParameterType().equals(Member.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        CurrentMember annotation = parameter.getParameterAnnotation(CurrentMember.class);
        boolean required = annotation == null || annotation.required();
        boolean includeSanctioned = annotation != null && annotation.includeSanctioned();

        try {
            return resolveCurrentMember(includeSanctioned);
        } catch (AuthException e) {
            if (required) {
                throw e;
            }
            return null;
        }
    }

    private Member resolveCurrentMember(boolean includeSanctioned) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorReason.UNAUTHORIZED);
        }

        if (!(authentication.getPrincipal() instanceof ClerkPrincipal clerkPrincipal)) {
            throw new AuthException(AuthErrorReason.UNAUTHORIZED);
        }

        String clerkId = clerkPrincipal.getClerkId();
        return (includeSanctioned
                ? memberRepository.findByClerkIdAndUsedIn(clerkId, SANCTIONED_INCLUDED_STATUSES)
                : memberRepository.findByClerkId(clerkId))
                .orElseThrow(() -> new AuthException(AuthErrorReason.NOT_REGISTERED));
    }
}
