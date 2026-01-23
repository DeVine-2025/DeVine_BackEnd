package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.command.MemberCommandService;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController implements MemberControllerDocs {
    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    // 닉네임 중복 체크
    @Override
    @GetMapping("/nickname/{nickname}")
    public ApiResponse<MemberResDTO.NicknameDuplicateDTO> checkNicknameDuplicate(
            @PathVariable("nickname") String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.NICKNAME_CHECKED;
        return ApiResponse.onSuccess(code, memberQueryService.checkNicknameDuplicate(nickname));
    }

    // 나에게 맞는 개발자 추천 (TODO: 페이지네이션)
    @Override
    @GetMapping("/recommend")
    public ApiResponse<MemberResDTO.DeveloperListDTO> getRecommendDevelopers() {
        MemberSuccessCode code = MemberSuccessCode.FOUND;

        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        return ApiResponse.onSuccess(code, memberQueryService.findAllDevelopers(memberId));
    }

    // TODO : 나에게 맞는 개발자 추천 (프리뷰)

    // 프로필 수정
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
