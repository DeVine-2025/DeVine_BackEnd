package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.WithdrawalReqDTO;
import com.umc.devine.domain.member.dto.WithdrawalResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.command.MemberWithdrawalCommandService;
import com.umc.devine.domain.member.service.command.MemberWithdrawalFacade;
import com.umc.devine.domain.member.service.query.MemberWithdrawalQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/me")
@Validated
public class MemberWithdrawalController implements MemberWithdrawalControllerDocs {

    private final MemberWithdrawalQueryService memberWithdrawalQueryService;
    private final MemberWithdrawalCommandService memberWithdrawalCommandService;
    private final MemberWithdrawalFacade memberWithdrawalFacade;

    @Override
    @GetMapping("/withdrawal-preview")
    public ApiResponse<WithdrawalResDTO.PreviewDTO> getWithdrawalPreview(
            @CurrentMember(includeSanctioned = true) Member member,
            @AuthenticationPrincipal ClerkPrincipal principal
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND_WITHDRAWAL_PREVIEW;
        WithdrawalResDTO.PreviewDTO response = memberWithdrawalQueryService.getPreview(member, principal);
        return ApiResponse.onSuccess(code, response);
    }

    @Override
    @PostMapping("/matchings/cancel-pending")
    public ApiResponse<WithdrawalResDTO.CancelPendingResultDTO> cancelPendingMatchings(
            @CurrentMember(includeSanctioned = true) Member member
    ) {
        MemberSuccessCode code = MemberSuccessCode.PENDING_MATCHINGS_CANCELED;
        WithdrawalResDTO.CancelPendingResultDTO response = memberWithdrawalCommandService.cancelPendingMatchings(member);
        return ApiResponse.onSuccess(code, response);
    }

    @Override
    @PostMapping("/withdrawal")
    public ApiResponse<WithdrawalResDTO.WithdrawResultDTO> withdraw(
            @CurrentMember(includeSanctioned = true) Member member,
            @RequestBody @Valid WithdrawalReqDTO.WithdrawReq request
    ) {
        MemberSuccessCode code = MemberSuccessCode.WITHDRAWN;
        WithdrawalResDTO.WithdrawResultDTO response = memberWithdrawalFacade.withdraw(member, request);
        return ApiResponse.onSuccess(code, response);
    }
}
