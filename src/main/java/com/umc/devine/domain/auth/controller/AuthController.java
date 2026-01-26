package com.umc.devine.domain.auth.controller;

import com.umc.devine.domain.auth.dto.AuthResDTO;
import com.umc.devine.domain.auth.exception.code.AuthSuccessCode;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.ClerkPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final MemberRepository memberRepository;

    @GetMapping("/health")
    public ApiResponse<AuthResDTO.HealthDTO> healthCheck() {
        AuthResDTO.HealthDTO response = AuthResDTO.HealthDTO.builder()
                .status("UP")
                .message("Auth service is running")
                .build();

        return ApiResponse.onSuccess(AuthSuccessCode.HEALTH_OK, response);
    }

    @GetMapping("/me")
    public ApiResponse<AuthResDTO.MeDTO> me(@AuthenticationPrincipal ClerkPrincipal principal) {
        String clerkId = principal.getClerkId();
        String email = principal.getEmail();

        Optional<Member> memberOptional = memberRepository.findByClerkId(clerkId);

        AuthResDTO.MeDTO response = AuthResDTO.MeDTO.builder()
                .clerkId(clerkId)
                .email(email)
                .memberId(memberOptional.map(Member::getId).orElse(null))
                .isRegistered(memberOptional.isPresent())
                .build();

        return ApiResponse.onSuccess(AuthSuccessCode.TOKEN_VALID, response);
    }
}
