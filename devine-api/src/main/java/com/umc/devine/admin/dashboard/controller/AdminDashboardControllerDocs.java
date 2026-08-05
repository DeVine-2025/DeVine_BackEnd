package com.umc.devine.admin.dashboard.controller;

import com.umc.devine.admin.dashboard.dto.AdminDashboardResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Dashboard", description = "관리자 대시보드 API")
public interface AdminDashboardControllerDocs {

    @Operation(summary = "대시보드 지표 조회 API",
            description = "관리자 홈 화면에 표시할 신고 대기 건수, 전체 쿠폰 사용률(%), 오늘 결제 건수를 조회합니다. " +
                    "특정 지표 조회에 실패해도 나머지 지표는 정상 반환하며, 실패한 지표만 null로 내려갑니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<AdminDashboardResDTO.DashboardDTO> getDashboard();
}
