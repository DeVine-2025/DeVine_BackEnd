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
import com.umc.devine.global.security.CurrentMember;
import com.umc.devine.global.validation.annotation.ValidNickname;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matching")
@Validated
public class MatchingController implements MatchingControllerDocs {

    private final MatchingCommandService matchingCommandService;
    private final MatchingQueryService matchingQueryService;

    @Override
    @PostMapping("/applications/projects/{projectId}")
    public ApiResponse<MatchingResDTO.ProposeResDTO> applyToProject(
            @CurrentMember Member member,
            @PathVariable Long projectId,
            @Valid @RequestBody MatchingReqDTO.ApplyReqDTO dto
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.applyToProject(member, projectId, dto.part());
        return ApiResponse.onSuccess(MatchingSuccessCode.APPLY_SUCCESS, result);
    }

    @Override
    @PatchMapping("/applications/projects/{projectId}")
    public ApiResponse<MatchingResDTO.ProposeResDTO> updateApplication(
            @CurrentMember Member member,
            @PathVariable Long projectId,
            @Valid @RequestBody MatchingReqDTO.ApplyReqDTO dto
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.updateApplicationPart(member, projectId, dto.part());
        return ApiResponse.onSuccess(MatchingSuccessCode.UPDATE_APPLICATION_SUCCESS, result);
    }

    @Override
    @PatchMapping("/applications/projects/{projectId}/cancel")
    public ApiResponse<MatchingResDTO.ProposeResDTO> cancelApplication(
            @CurrentMember Member member,
            @PathVariable Long projectId
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.cancelApplication(member, projectId);
        return ApiResponse.onSuccess(MatchingSuccessCode.CANCEL_SUCCESS, result);
    }

    @Override
    @PatchMapping("/applications/{matchingId}/respond")
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
    @PostMapping("/proposals/members/{nickname}")
    public ApiResponse<MatchingResDTO.ProposeResDTO> proposeToMember(
            @CurrentMember Member member,
            @PathVariable @ValidNickname String nickname,
            @Valid @RequestBody MatchingReqDTO.ProposeReqDTO dto
    ) {
        MatchingResDTO.ProposeResDTO result = matchingCommandService.proposeToMember(member, nickname, dto.projectId(), dto.part(), dto.content());
        return ApiResponse.onSuccess(MatchingSuccessCode.PROPOSE_SUCCESS, result);
    }

    @Override
    @PatchMapping("/proposals/{matchingId}/respond")
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
    @GetMapping("/pm/proposed-developers")
    public ApiResponse<MatchingResDTO.DevelopersRes> getProposedDevelopers(
            @CurrentMember Member member,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        MatchingResDTO.DevelopersRes result = matchingQueryService.getDevelopers(member, MatchingType.PROPOSE, pageable);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_DEVELOPERS_SUCCESS, result);
    }

    @Override
    @GetMapping("/pm/applications")
    public ApiResponse<MatchingResDTO.DevelopersRes> getPmApplications(
            @CurrentMember Member member,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        MatchingResDTO.DevelopersRes result = matchingQueryService.getDevelopers(member, MatchingType.APPLY, pageable);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_DEVELOPERS_SUCCESS, result);
    }

    @Override
    @GetMapping("/developer/received-proposals")
    public ApiResponse<MatchingResDTO.ProjectsRes> getReceivedProposals(
            @CurrentMember Member member,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        MatchingResDTO.ProjectsRes result = matchingQueryService.getProjects(member, MatchingType.PROPOSE, pageable);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_PROJECTS_SUCCESS, result);
    }

    @Override
    @GetMapping("/developer/applications")
    public ApiResponse<MatchingResDTO.ProjectsRes> getDeveloperApplications(
            @CurrentMember Member member,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        MatchingResDTO.ProjectsRes result = matchingQueryService.getProjects(member, MatchingType.APPLY, pageable);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_PROJECTS_SUCCESS, result);
    }

    // 내 지원/개발 상태 조회(버튼 체크 용도)

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
    @GetMapping("/projects/{projectId}/propose/{nickname}")
    public ApiResponse<MatchingResDTO.MatchingStatusRes> getMyProposeStatus(
            @CurrentMember Member member,
            @PathVariable Long projectId,
            @PathVariable @ValidNickname String nickname
    ) {
        MatchingResDTO.MatchingStatusRes result =
                matchingQueryService.getMyProposeStatus(member, projectId, nickname);
        return ApiResponse.onSuccess(MatchingSuccessCode.GET_APPLICANT_STATUS_SUCCESS, result);
    }
}
