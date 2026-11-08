package com.umc.devine.domain.complaint.controller;

import com.umc.devine.domain.complaint.dto.ComplaintReqDTO;
import com.umc.devine.domain.complaint.dto.ComplaintResDTO;
import com.umc.devine.domain.complaint.exception.code.ComplaintSuccessCode;
import com.umc.devine.domain.complaint.service.command.ComplaintCommandService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/complaints")
@Validated
public class ComplaintController implements ComplaintControllerDocs {

    private final ComplaintCommandService complaintCommandService;

    @Override
    @PostMapping
    public ApiResponse<ComplaintResDTO.CreateRes> createComplaint(
            @CurrentMember Member member,
            @Valid @RequestBody ComplaintReqDTO.CreateReq request
    ) {
        ComplaintResDTO.CreateRes response = complaintCommandService.create(member, request);
        return ApiResponse.onSuccess(ComplaintSuccessCode.COMPLAINT_CREATED, response);
    }
}
