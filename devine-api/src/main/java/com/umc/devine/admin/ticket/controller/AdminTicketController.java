package com.umc.devine.admin.ticket.controller;

import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.admin.ticket.dto.AdminTicketReqDTO;
import com.umc.devine.admin.ticket.dto.AdminTicketResDTO;
import com.umc.devine.admin.ticket.exception.code.AdminTicketSuccessCode;
import com.umc.devine.admin.ticket.service.command.AdminTicketCommandService;
import com.umc.devine.admin.ticket.service.query.AdminTicketQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/ticket")
@Validated
public class AdminTicketController implements AdminTicketControllerDocs {

    private final AdminTicketQueryService adminTicketQueryService;
    private final AdminTicketCommandService adminTicketCommandService;

    @Override
    @GetMapping("/refunds")
    public ApiResponse<PagedResponse<AdminTicketResDTO.RefundRequestDTO>> getRefundRequests(
            @ParameterObject @ModelAttribute @Valid AdminTicketReqDTO.RefundRequestSearchReq request
    ) {
        PagedResponse<AdminTicketResDTO.RefundRequestDTO> response = adminTicketQueryService.getRefundRequests(request);
        return ApiResponse.onSuccess(AdminTicketSuccessCode.REFUND_REQUEST_LIST_FOUND, response);
    }

    @Override
    @PatchMapping("/refunds/{refundRequestId}")
    public ApiResponse<AdminTicketResDTO.ProcessRefundRes> processRefundRequest(
            @AuthenticationPrincipal AdminPrincipal admin,
            @PathVariable Long refundRequestId
    ) {
        AdminTicketResDTO.ProcessRefundRes response = adminTicketCommandService.processRefundRequest(refundRequestId, admin.getClerkId());
        return ApiResponse.onSuccess(AdminTicketSuccessCode.REFUND_REQUEST_PROCESSED, response);
    }
}
