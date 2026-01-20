package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.command.MemberCommandService;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.domain.techstack.enums.TechGenre;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
// TODO: 도메인 분리하기 member, members
@RequestMapping("/api/v1/member")
public class MemberController implements MemberControllerDocs {
    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    @Override
    @GetMapping("/")
    public ApiResponse<MemberResDTO.MemberDetailDTO> getMember(){
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findMemberById(memberId));
    }

    @Override
    @GetMapping("/{nickname}")
    public ApiResponse<MemberResDTO.UserProfileDTO> getMemberByNickname(@PathVariable("nickname") String nickname) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, memberQueryService.findMemberByNickname(nickname));
    }

    @Override
    @GetMapping("/projects")
    public ApiResponse<ProjectResDTO.ProjectListDTO> getProjects() {
        MemberSuccessCode code = MemberSuccessCode.FOUND_PROJECT;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findMyProjects(memberId));
    }

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

    @Override
    @GetMapping("/nickname/{nickname}")
    public ApiResponse<MemberResDTO.NicknameDuplicateDTO> checkNicknameDuplicate(
            @PathVariable("nickname") String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.NICKNAME_CHECKED;
        return ApiResponse.onSuccess(code, memberQueryService.checkNicknameDuplicate(nickname));
    }

    @Override
    @GetMapping("/reports")
    public ApiResponse<DevReportResDTO.ReportListDTO> getMyReports() {
        MemberSuccessCode code = MemberSuccessCode.FOUND_REPORT;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findMyReports(memberId));
    }

    @Override
    @GetMapping("/{nickname}/reports")
    public ApiResponse<DevReportResDTO.ReportListDTO> getReportsByNickname(
            @PathVariable("nickname") String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND_REPORT;
        return ApiResponse.onSuccess(code, memberQueryService.findReportsByNickname(nickname));
    }

    @Override
    @GetMapping("/contributions")
    public ApiResponse<MemberResDTO.ContributionListDTO> getContribution() {
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findContributionsById(memberId));
    }

    @Override
    @GetMapping("/{nickname}/contributions")
    public ApiResponse<MemberResDTO.ContributionListDTO> getContributionByNickname(
            @PathVariable("nickname") String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, memberQueryService.findContributionsByNickname(nickname));
    }

    @Override
    @GetMapping("/recommend")
    public ApiResponse<MemberResDTO.DeveloperListDTO> getRecommendDevelopers() {
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findAllDevelopers(memberId));
    }

    @Override
    @GetMapping("/search")
    public ApiResponse<MemberResDTO.UserProfileListDTO> searchDevelopers(
            @RequestParam(value = "category", required = false) CategoryGenre category,
            @RequestParam(value = "tech_genre", required = false) TechGenre techGenre,
            @RequestParam(value = "techstack_name", required = false) TechName techstackName
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, memberQueryService.searchDevelopers(category, techGenre, techstackName));
    }
}
