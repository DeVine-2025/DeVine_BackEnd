package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.project.dto.matching.MatchingReqDTO;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.exception.code.MatchingSuccessCode;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
import com.umc.devine.global.apiPayload.ApiResponse;
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
    public ApiResponse<MatchingResDTO.ProposeResDTO> applyToProject(@PathVariable Long projectId) {
        // TODO: SecurityContextHolder에서 Member 추출
        Long memberId = 2L;

        MatchingResDTO.ProposeResDTO result = matchingCommandService.applyToProject(memberId, projectId);
        return ApiResponse.onSuccess(MatchingSuccessCode.APPLY_SUCCESS, result);
    }

    @Override
    @PatchMapping("/projects/{projectId}")
    public ApiResponse<MatchingResDTO.ProposeResDTO> cancelApplication(@PathVariable Long projectId) {
        // TODO: SecurityContextHolder에서 Member 추출
        Long memberId = 2L;

        MatchingResDTO.ProposeResDTO result = matchingCommandService.cancelApplication(memberId, projectId);
        return ApiResponse.onSuccess(MatchingSuccessCode.CANCEL_SUCCESS, result);
    }

    @Override
    @PostMapping("/members/{nickname}")
    public ApiResponse<MatchingResDTO.ProposeResDTO> proposeToMember(
            @PathVariable String nickname,
            @Valid @RequestBody MatchingReqDTO.ProposeReqDTO dto
    ) {
        // TODO: SecurityContextHolder에서 Member 추출
        Long memberId = 1L;

        MatchingResDTO.ProposeResDTO result = matchingCommandService.proposeToMember(memberId, nickname, dto.projectId());
        return ApiResponse.onSuccess(MatchingSuccessCode.PROPOSE_SUCCESS, result);
    }
}
