package com.umc.devine.admin.auth.controller;

import com.umc.devine.admin.auth.dto.AdminAuthResDTO;
import com.umc.devine.admin.auth.exception.code.AdminAuthSuccessCode;
import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/auth")
public class AdminAuthController implements AdminAuthControllerDocs {

    @Override
    @GetMapping("/me")
    public ApiResponse<AdminAuthResDTO.MeDTO> me(@AuthenticationPrincipal AdminPrincipal principal) {
        AdminAuthResDTO.MeDTO response = AdminAuthResDTO.MeDTO.builder()
                .clerkId(principal.getClerkId())
                .email(principal.getEmail())
                .level(principal.getLevel())
                .build();

        return ApiResponse.onSuccess(AdminAuthSuccessCode.ADMIN_ME_OK, response);
    }
}