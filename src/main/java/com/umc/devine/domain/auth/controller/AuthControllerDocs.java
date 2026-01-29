package com.umc.devine.domain.auth.controller;

import com.umc.devine.domain.auth.dto.AuthResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.ClerkPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthControllerDocs {

    @Operation(summary = "헬스 체크 API", description = "Auth 서비스의 상태를 확인하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<AuthResDTO.HealthDTO> healthCheck();

    @Operation(summary = "내 정보 조회 API", description = "현재 로그인한 사용자의 정보를 조회하는 API입니다. Clerk 토큰을 기반으로 사용자 정보를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.")
    })
    ApiResponse<AuthResDTO.MeDTO> me(@AuthenticationPrincipal ClerkPrincipal principal);
}
