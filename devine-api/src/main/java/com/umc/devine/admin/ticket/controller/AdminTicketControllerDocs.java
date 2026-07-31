package com.umc.devine.admin.ticket.controller;

import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.ticket.dto.AdminTicketReqDTO;
import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Admin Ticket", description = "회원 자진 탈퇴 시 잔여 리포트 생성권 환불 신청 관리 API")
public interface AdminTicketControllerDocs {

    @Operation(summary = "환불 신청 목록 조회", description = "회원 자진 탈퇴 시 접수된 잔여 리포트 생성권 환불 신청 목록을 상태별로 필터링하여 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "환불 신청 목록 조회 성공")
    })
    ApiResponse<PagedResponse<AdminTicketResDTO.RefundRequestDTO>> getRefundRequests(
            @ParameterObject @ModelAttribute @Valid AdminTicketReqDTO.RefundRequestSearchReq request
    );

    @Operation(summary = "환불 신청 처리완료 처리", description = "관리자가 실제 환불 처리를 마친 뒤 해당 환불 신청을 처리완료 상태로 전환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리완료 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 처리완료된 환불 신청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 환불 신청을 찾을 수 없습니다.")
    })
    ApiResponse<AdminTicketResDTO.ProcessRefundRes> processRefundRequest(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long refundRequestId
    );
}
