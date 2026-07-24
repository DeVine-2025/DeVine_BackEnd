package com.umc.devine.admin.member.controller;

import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.admin.member.exception.code.AdminMemberSuccessCode;
import com.umc.devine.admin.member.service.command.AdminMemberCommandService;
import com.umc.devine.admin.member.service.query.AdminMemberQueryService;
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
@RequestMapping("/admin/v1/member")
@Validated
public class AdminMemberController implements AdminMemberControllerDocs {

    private final AdminMemberQueryService adminMemberQueryService;
    private final AdminMemberCommandService adminMemberCommandService;

    @Override
    @GetMapping
    public ApiResponse<PagedResponse<AdminMemberResDTO.MemberSummaryDTO>> getMemberList(
            @ParameterObject @ModelAttribute @Valid AdminMemberReqDTO.SearchReq request
    ) {
        PagedResponse<AdminMemberResDTO.MemberSummaryDTO> response = adminMemberQueryService.getMemberList(request);
        return ApiResponse.onSuccess(AdminMemberSuccessCode.MEMBER_LIST_FOUND, response);
    }

    @Override
    @GetMapping("/{nickname}")
    public ApiResponse<AdminMemberResDTO.MemberDetailRes> getMemberDetail(@PathVariable String nickname) {
        AdminMemberResDTO.MemberDetailRes response = adminMemberQueryService.getMemberDetail(nickname);
        return ApiResponse.onSuccess(AdminMemberSuccessCode.MEMBER_DETAIL_FOUND, response);
    }

    @Override
    @PatchMapping("/{nickname}/status")
    public ApiResponse<AdminMemberResDTO.ChangeStatusRes> changeStatus(
            @CurrentMember(required = false) Member member,
            @PathVariable String nickname,
            @RequestBody @Valid AdminMemberReqDTO.ChangeStatusReq request
    ) {
        Long processorMemberId = member != null ? member.getId() : null;
        AdminMemberResDTO.ChangeStatusRes response = adminMemberCommandService.changeStatus(nickname, processorMemberId, request);
        return ApiResponse.onSuccess(AdminMemberSuccessCode.STATUS_CHANGED, response);
    }
}
