package com.umc.devine.domain.report.controller;

import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Report", description = "리포트 관련 API")
public interface ReportControllerDocs {

    @Operation(summary = "메인 리포트 조회", description = "Git 저장소의 메인 리포트를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트를 찾을 수 없음")
    })
    ApiResponse<ReportResDTO.ReportRes> getMainReport(
            @Parameter(description = "Git 저장소 ID", required = true) @PathVariable Long gitRepoId
    );

    @Operation(summary = "상세 리포트 조회", description = "Git 저장소의 상세 리포트를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트를 찾을 수 없음")
    })
    ApiResponse<ReportResDTO.ReportRes> getDetailReport(
            @Parameter(description = "Git 저장소 ID", required = true) @PathVariable Long gitRepoId
    );

    @Operation(summary = "리포트 공개 범위 수정", description = "리포트의 공개 범위를 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공개 범위 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리포트에 대한 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트를 찾을 수 없음")
    })
    ApiResponse<ReportResDTO.UpdateVisibilityRes> updateVisibility(
            @Parameter(description = "리포트 ID", required = true) @PathVariable Long reportId,
            @RequestBody @Valid ReportReqDTO.UpdateVisibilityReq request
    );
}
