package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.CurrentMember;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController implements MemberControllerDocs {
    private final MemberQueryService memberQueryService;

    // 나에게 맞는 개발자 추천
    @Override
    @GetMapping("/recommend")
    public ApiResponse<PagedResponse<MemberResDTO.DeveloperDTO>> getRecommendDevelopers(
            @CurrentMember Member member,
            @ParameterObject @ModelAttribute MemberReqDTO.RecommendDeveloperDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, memberQueryService.findAllDevelopers(member, dto));
    }

    // 나에게 맞는 개발자 추천 (프리뷰)
    @Override
    @GetMapping("/recommend/preview")
    public ApiResponse<List<MemberResDTO.DeveloperDTO>> getRecommendDevelopersPreview(
            @CurrentMember Member member,
            @RequestParam(defaultValue = "4") int limit
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, memberQueryService.findAllDevelopersPreview(member, limit));
    }

    // 특정 회원 프로필 조회
    @Override
    @GetMapping("/{nickname}")
    public ApiResponse<MemberResDTO.UserProfileDTO> getMemberByNickname(@PathVariable("nickname") String nickname) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, memberQueryService.findMemberByNickname(nickname));
    }

    // 개발자 필터링 검색 (페이지네이션)
    @Override
    @GetMapping("/search")
    public ApiResponse<PagedResponse<MemberResDTO.UserProfileDTO>> searchDevelopers(
            @ParameterObject @ModelAttribute MemberReqDTO.SearchDeveloperDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, memberQueryService.searchDevelopers(dto));
    }

    // 사용자 깃허브 기록
    @Override
    @GetMapping("/{nickname}/contributions")
    public ApiResponse<MemberResDTO.ContributionListDTO> getContributionByNickname(
            @PathVariable("nickname") String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, memberQueryService.findContributionsByNickname(nickname));
    }

    // 사용자 리포트
    @Override
    @GetMapping("/{nickname}/reports")
    public ApiResponse<DevReportResDTO.ReportListDTO> getReportsByNickname(
            @PathVariable("nickname") String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND_REPORT;
        return ApiResponse.onSuccess(code, memberQueryService.findReportsByNickname(nickname));
    }
}
