package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.auth.service.AuthHelper;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.command.MemberCommandService;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.ClerkPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
public class MyProfileController implements MyProfileControllerDocs {
    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;
    private final AuthHelper authHelper;

    // 내 프로필 조회
    @Override
    @GetMapping("/")
    public ApiResponse<MemberResDTO.MemberProfileDTO> getMemberProfile(@AuthenticationPrincipal ClerkPrincipal principal){

        Member member = authHelper.getMember(principal);
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        return ApiResponse.onSuccess(code, memberQueryService.findMemberProfile(member));
    }

    // 내 프로필 수정
    @Override
    @PatchMapping("/")
    public ApiResponse<MemberResDTO.MemberProfileDTO> patchMember(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @RequestBody @Valid MemberReqDTO.UpdateMemberDTO dto
    ){

        Member member = authHelper.getMember(principal);
        MemberSuccessCode code = MemberSuccessCode.UPDATED;

        return ApiResponse.onSuccess(code, memberCommandService.updateMember(member, dto));
    }

    // 내 보유 기술 조회
    @Override
    @GetMapping("/techstacks")
    public ApiResponse<TechstackResDTO.DevTechstackListDTO> getMyTechstacks(@AuthenticationPrincipal ClerkPrincipal principal) {
        Member member = authHelper.getMember(principal);
        MemberSuccessCode code = MemberSuccessCode.FOUND_TECHSTACK;

        return ApiResponse.onSuccess(code, memberQueryService.findMemberTechstacks(member));
    }
    
    // 내 보유 기술 추가
    @Override
    @PostMapping("/techstacks")
    public ApiResponse<TechstackResDTO.DevTechstackListDTO> addMyTechstacks(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @RequestBody @Valid MemberReqDTO.AddTechstackDTO dto
    ) {
        Member member = authHelper.getMember(principal);
        MemberSuccessCode code = MemberSuccessCode.CREATED_TECHSTACK;

        return ApiResponse.onSuccess(code, memberCommandService.addMemberTechstacks(member, dto));
    }

    // 내 보유 기술 삭제
    @Override
    @DeleteMapping("/techstacks")
    public ApiResponse<TechstackResDTO.DevTechstackListDTO> removeMyTechstacks(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @RequestBody @Valid MemberReqDTO.RemoveTechstackDTO dto
    ) {
        Member member = authHelper.getMember(principal);
        MemberSuccessCode code = MemberSuccessCode.DELETED_TECHSTACK;

        return ApiResponse.onSuccess(code, memberCommandService.removeMemberTechstacks(member, dto));
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

    // 내 프로젝트 조회
    @Override
    @GetMapping("/projects")
    public ApiResponse<ProjectResDTO.ProjectListDTO> getProjects(@AuthenticationPrincipal ClerkPrincipal principal){
        Member member = authHelper.getMember(principal);
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        return ApiResponse.onSuccess(code, memberQueryService.findMyProjects(member));
    }

    // 내 리포트 조회
    @Override
    @GetMapping("/reports")
    public ApiResponse<DevReportResDTO.ReportListDTO> getMyReports(@AuthenticationPrincipal ClerkPrincipal principal) {
        Member member = authHelper.getMember(principal);
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        return ApiResponse.onSuccess(code, memberQueryService.findMyReports(member));
    }
}
