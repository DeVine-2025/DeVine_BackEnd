package com.umc.devine.admin.complaint.controller;

import com.umc.devine.admin.complaint.dto.ComplaintReqDTO;
import com.umc.devine.admin.complaint.dto.ComplaintResDTO;
import com.umc.devine.admin.complaint.exception.code.ComplaintSuccessCode;
import com.umc.devine.admin.complaint.service.command.ComplaintCommandService;
import com.umc.devine.admin.complaint.service.query.ComplaintQueryService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/complaints")
@Validated
public class ComplaintController implements ComplaintControllerDocs {

    private final ComplaintQueryService complaintQueryService;
    private final ComplaintCommandService complaintCommandService;

    @Override
    @GetMapping
    public ApiResponse<PagedResponse<ComplaintResDTO.ComplaintSummaryDTO>> getComplaintList(
            @ParameterObject @ModelAttribute @Valid ComplaintReqDTO.SearchReq request
    ) {
        PagedResponse<ComplaintResDTO.ComplaintSummaryDTO> response = complaintQueryService.getComplaintList(request);
        return ApiResponse.onSuccess(ComplaintSuccessCode.COMPLAINT_LIST_FOUND, response);
    }

    @Override
    @GetMapping("/{complaintId}")
    public ApiResponse<ComplaintResDTO.ComplaintDetailRes> getComplaintDetail(@PathVariable Long complaintId) {
        ComplaintResDTO.ComplaintDetailRes response = complaintQueryService.getComplaintDetail(complaintId);
        return ApiResponse.onSuccess(ComplaintSuccessCode.COMPLAINT_DETAIL_FOUND, response);
    }

    @Override
    @PatchMapping("/{complaintId}/status")
    public ApiResponse<ComplaintResDTO.UpdateStatusRes> updateStatus(
            @CurrentMember(required = false) Member member,
            @PathVariable Long complaintId,
            @RequestBody @Valid ComplaintReqDTO.UpdateStatusReq request
    ) {
        Long processorMemberId = member != null ? member.getId() : null;
        ComplaintResDTO.UpdateStatusRes response = complaintCommandService.updateStatus(complaintId, processorMemberId, request);
        return ApiResponse.onSuccess(ComplaintSuccessCode.STATUS_UPDATED, response);
    }
}
