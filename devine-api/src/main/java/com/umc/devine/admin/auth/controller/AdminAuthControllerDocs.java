package com.umc.devine.admin.auth.controller;

import com.umc.devine.admin.auth.dto.AdminAuthResDTO;
import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Admin Auth", description = "관리자 인증 관련 API")
public interface AdminAuthControllerDocs {

    @Operation(summary = "관리자 내 정보 조회 API",
            description = "현재 로그인한 관리자의 정보(권한 레벨 포함)를 조회합니다. Clerk 토큰 기반이며 ROLE_ADMIN이 필요합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한이 없습니다.")
    })
    ApiResponse<AdminAuthResDTO.MeDTO> me(@AuthenticationPrincipal AdminPrincipal principal);
}