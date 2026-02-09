package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.matching.MatchingReqDTO;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import com.umc.devine.domain.project.exception.code.MatchingSuccessCode;
import com.umc.devine.domain.project.service.command.MatchingCommandService;
import com.umc.devine.domain.project.service.query.MatchingQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matching")
public class MatchingController implements MatchingControllerDocs {

    private final MatchingCommandService matchingCommandService;
    private final MatchingQueryService matchingQueryService;

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

    @Override
    @PatchMapping("/{matchingId}/application/respond")
    public ApiResponse<MatchingResDTO.ProposeResDTO> respondToApplication(
            @CurrentMember Member member,
            @PathVariable Long matchingId,
            @Valid @RequestBody MatchingReqDTO.DecisionReqDTO dto
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.respondToApplication(member, matchingId, dto.decision());
        MatchingSuccessCode successCode = dto.decision() == MatchingDecision.ACCEPT
                ? MatchingSuccessCode.APPLICATION_ACCEPTED
                : MatchingSuccessCode.APPLICATION_REJECTED;
        return ApiResponse.onSuccess(successCode, result);
    }

    @Override
    @PatchMapping("/{matchingId}/proposal/respond")
    public ApiResponse<MatchingResDTO.ProposeResDTO> respondToProposal(
            @CurrentMember Member member,
            @PathVariable Long matchingId,
            @Valid @RequestBody MatchingReqDTO.DecisionReqDTO dto
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.respondToProposal(member, matchingId, dto.decision());
        MatchingSuccessCode successCode = dto.decision() == MatchingDecision.ACCEPT
                ? MatchingSuccessCode.PROPOSAL_ACCEPTED
                : MatchingSuccessCode.PROPOSAL_REJECTED;
        return ApiResponse.onSuccess(successCode, result);
    }

    @Override
    @GetMapping("/pm/developers")
    public ApiResponse<MatchingResDTO.DevelopersRes> getDevelopers(
            @CurrentMember Member member,
            @RequestParam MatchingType type,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        MatchingResDTO.DevelopersRes result = matchingQueryService.getDevelopers(member, type, pageable);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_DEVELOPERS_SUCCESS, result);
    }

    @Override
    @GetMapping("/developer/projects")
    public ApiResponse<MatchingResDTO.ProjectsRes> getProjects(
            @CurrentMember Member member,
            @RequestParam MatchingType type,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        MatchingResDTO.ProjectsRes result = matchingQueryService.getProjects(member, type, pageable);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_PROJECTS_SUCCESS, result);
    }

    @Override
    @GetMapping("/projects/{projectId}/my-apply")
    public ApiResponse<MatchingResDTO.MatchingStatusRes> getMyApplyStatus(
            @CurrentMember Member member,
            @PathVariable Long projectId
    ) {
        MatchingResDTO.MatchingStatusRes result =
                matchingQueryService.getMyApplyStatus(member, projectId);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_MY_MATCHING_STATUS_SUCCESS, result);
    }

    @Override
    @GetMapping("/projects/{projectId}/propose/{memberId}")
    public ApiResponse<MatchingResDTO.MatchingStatusRes> getMyProposeStatus(
            @CurrentMember Member member,
            @PathVariable Long projectId,
            @PathVariable Long memberId
    ) {
        MatchingResDTO.MatchingStatusRes result =
                matchingQueryService.getMyProposeStatus(member, projectId, memberId);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_APPLICANT_STATUS_SUCCESS, result);
    }
}
