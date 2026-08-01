package com.umc.devine.admin.complaint.controller;

import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Complaint", description = "관리자 신고/제재 관리 API")
public interface ComplaintControllerDocs {

    @Operation(summary = "신고 목록 조회", description = "채팅/프로젝트/개발자 유형별, 상태별, 날짜별로 신고 목록을 조회합니다. 접수 후 48시간 초과 미처리 건은 slaExceeded로 표시됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 목록 조회 성공")
    })
    ApiResponse<PagedResponse<ComplaintResDTO.ComplaintSummaryDTO>> getComplaintList(
            @ParameterObject @ModelAttribute @Valid ComplaintReqDTO.SearchReq request
    );

    @Operation(summary = "신고 상세 조회", description = "신고 상세 정보, 관련 콘텐츠 원문, 피신고자의 누적 신고/제재 이력을 함께 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고를 찾을 수 없음")
    })
    ApiResponse<ComplaintResDTO.ComplaintDetailRes> getComplaintDetail(
            @Parameter(description = "신고 ID", required = true) @PathVariable Long complaintId
    );

    @Operation(summary = "신고 처리 상태 변경", description = "대기→검토중→처리완료(경고/삭제/정지/기각)로 상태를 변경합니다. 처리완료 시 세부 액션과 처리 사유가 필수입니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "세부 액션 또는 처리 사유 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고를 찾을 수 없음")
    })
    ApiResponse<ComplaintResDTO.UpdateStatusRes> updateStatus(
            @Parameter(hidden = true) @AuthenticationPrincipal AdminPrincipal admin,
            @Parameter(description = "신고 ID", required = true) @PathVariable Long complaintId,
            @RequestBody @Valid ComplaintReqDTO.UpdateStatusReq request
    );
}
