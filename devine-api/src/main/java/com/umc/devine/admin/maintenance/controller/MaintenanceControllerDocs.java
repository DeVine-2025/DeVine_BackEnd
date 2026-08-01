package com.umc.devine.admin.maintenance.controller;

import com.umc.devine.admin.maintenance.dto.MaintenanceReqDTO;
import com.umc.devine.admin.maintenance.dto.MaintenanceResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Maintenance", description = "관리자 점검 모드 관리 API")
public interface MaintenanceControllerDocs {

    @Operation(summary = "점검 모드 상태 조회", description = "현재 점검 모드 on/off 상태와 안내 메시지, 예상 종료 시각을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<MaintenanceResDTO> getState();

    @Operation(summary = "점검 모드 전환", description = "점검 모드를 켜거나 끕니다. 끄면 안내 메시지와 예상 종료 시각도 함께 비워집니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "enabled 필드 누락 등 잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<MaintenanceResDTO> update(@RequestBody MaintenanceReqDTO.UpdateDTO request);
}
