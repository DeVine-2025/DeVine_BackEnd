package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.command.MemberCommandService;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
public class MyProfileController implements MyProfileControllerDocs {
    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    // 내 프로필 조회
    @Override
    @GetMapping("/")
    public ApiResponse<MemberResDTO.MemberDetailDTO> getMember(){
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findMemberById(memberId));
    }

    // 내 프로필 수정
    @Override
    @PatchMapping("/")
    public ApiResponse<MemberResDTO.MemberDetailDTO> patchMember(
            @RequestBody @Valid MemberReqDTO.UpdateMemberDTO dto
    ){
        MemberSuccessCode code = MemberSuccessCode.UPDATED;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberCommandService.updateMember(memberId, dto));
    }

    // TODO : 내 보유 기술 조회
    
    // 내 프로젝트 조회
    @Override
    @GetMapping("/projects")
    public ApiResponse<ProjectResDTO.ProjectListDTO> getProjects() {
        MemberSuccessCode code = MemberSuccessCode.FOUND_PROJECT;

        // TODO: 토큰 방식으로 변경
        Long memberId = 3L;

        return ApiResponse.onSuccess(code, memberQueryService.findMyProjects(memberId));
    }

    // 내 깃허브 기록
    @Override
    @GetMapping("/contributions")
    public ApiResponse<MemberResDTO.ContributionListDTO> getContribution() {
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findContributionsById(memberId));
    }
    
    // 내 리포트 조회
    @Override
    @GetMapping("/reports")
    public ApiResponse<DevReportResDTO.ReportListDTO> getMyReports() {
        MemberSuccessCode code = MemberSuccessCode.FOUND_REPORT;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findMyReports(memberId));
    }
}
