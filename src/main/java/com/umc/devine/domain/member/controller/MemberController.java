package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import com.umc.devine.global.validation.annotation.ValidNickname;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@Validated
public class MemberController implements MemberControllerDocs {

    private final MemberQueryService memberQueryService;

    // 나에게 맞는 개발자 추천
    @Override
    @GetMapping("/recommend")
    public ApiResponse<PagedResponse<MemberResDTO.RecommendedDeveloperDTO>> getRecommendDevelopers(
            @CurrentMember Member member,
            @ParameterObject @ModelAttribute MemberReqDTO.RecommendDeveloperDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        PagedResponse<MemberResDTO.RecommendedDeveloperDTO> response = memberQueryService.findRecommendedDevelopers(member, dto);
        return ApiResponse.onSuccess(code, response);
    }

    // 나에게 맞는 개발자 추천 (프리뷰)
    @Override
    @GetMapping("/recommend/preview")
    public ApiResponse<List<MemberResDTO.RecommendedDeveloperDTO>> getRecommendDevelopersPreview(
            @CurrentMember Member member,
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "4") int limit
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        List<MemberResDTO.RecommendedDeveloperDTO> response = memberQueryService.findRecommendedDevelopersPreview(member, projectId, limit);
        return ApiResponse.onSuccess(code, response);
    }

    // 특정 회원 프로필 조회
    @Override
    @GetMapping("/{nickname}")
    public ApiResponse<MemberResDTO.MemberProfileDTO> getMemberByNickname(
            @PathVariable("nickname") @ValidNickname String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        MemberResDTO.MemberProfileDTO response = memberQueryService.findMemberByNickname(nickname);
        return ApiResponse.onSuccess(code, response);
    }

    // 개발자 필터링 검색 (페이지네이션)
    @Override
    @GetMapping("/search")
    public ApiResponse<PagedResponse<MemberResDTO.MemberListItemDTO>> searchDevelopers(
            @ParameterObject @ModelAttribute MemberReqDTO.SearchDeveloperDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        PagedResponse<MemberResDTO.MemberListItemDTO> response = memberQueryService.searchDevelopers(dto);
        return ApiResponse.onSuccess(code, response);
    }

    // 사용자 깃허브 기록
    @Override
    @GetMapping("/{nickname}/contributions")
    public ApiResponse<MemberResDTO.ContributionListDTO> getContributionByNickname(
            @PathVariable("nickname") @ValidNickname String nickname,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND_CONTRIBUTIONS;
        MemberResDTO.ContributionListDTO response = memberQueryService.findContributionsByNickname(nickname, from, to);
        return ApiResponse.onSuccess(code, response);
    }

    // 특정 회원 기술스택 조회
    @Override
    @GetMapping("/{nickname}/techstacks")
    public ApiResponse<TechstackResDTO.DevTechstackListDTO> getTechstacksByNickname(
            @PathVariable("nickname") String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND_TECHSTACK;
        TechstackResDTO.DevTechstackListDTO response = memberQueryService.findTechstacksByNickname(nickname);
        return ApiResponse.onSuccess(code, response);
    }

}
