package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.matching.MatchingReqDTO;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.exception.code.MatchingSuccessCode;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matching")
public class MatchingController implements MatchingControllerDocs {

    private final MatchingCommandService matchingCommandService;

    @Override
    @PostMapping("/projects/{projectId}")
    public ApiResponse<MatchingResDTO.ProposeResDTO> applyToProject(
            @CurrentMember Member member,
            @PathVariable Long projectId
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.applyToProject(member, projectId);
        return ApiResponse.onSuccess(MatchingSuccessCode.APPLY_SUCCESS, result);
    }

    @Override
    @PatchMapping("/projects/{projectId}")
    public ApiResponse<MatchingResDTO.ProposeResDTO> cancelApplication(
            @CurrentMember Member member,
            @PathVariable Long projectId
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.cancelApplication(member, projectId);
        return ApiResponse.onSuccess(MatchingSuccessCode.CANCEL_SUCCESS, result);
    }

    @Override
    @PostMapping("/members/{nickname}")
    public ApiResponse<MatchingResDTO.ProposeResDTO> proposeToMember(
            @CurrentMember Member member,
            @PathVariable String nickname,
            @Valid @RequestBody MatchingReqDTO.ProposeReqDTO dto
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.proposeToMember(member, nickname, dto.projectId());
        return ApiResponse.onSuccess(MatchingSuccessCode.PROPOSE_SUCCESS, result);
    }
}
