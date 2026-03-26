package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.command.MemberCommandService;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
@Validated
public class MyProfileController implements MyProfileControllerDocs {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    // 이용약관 조회
    @Override
    @GetMapping("/terms")
    public ApiResponse<MemberResDTO.TermsListDTO> getTerms() {
        MemberSuccessCode code = MemberSuccessCode.FOUND_TERMS;
        MemberResDTO.TermsListDTO response = memberQueryService.findAllTerms();
        return ApiResponse.onSuccess(code, response);
    }

    // 회원가입
    @Override
    @PostMapping("/signup")
    public ApiResponse<MemberResDTO.SignupResultDTO> signup(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @RequestBody @Valid MemberReqDTO.SignupDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.SIGNUP_SUCCESS;
        MemberResDTO.SignupResultDTO response = memberCommandService.signup(principal, dto);
        return ApiResponse.onSuccess(code, response);
    }

    // 닉네임 중복 체크
    @Override
    @GetMapping("/nickname/check")
    public ApiResponse<MemberResDTO.NicknameDuplicateDTO> checkNicknameDuplicate(
            @RequestParam("nickname") String nickname
    ) {
        MemberSuccessCode code = MemberSuccessCode.NICKNAME_CHECKED;
        MemberResDTO.NicknameDuplicateDTO response = memberQueryService.checkNicknameDuplicate(nickname);
        return ApiResponse.onSuccess(code, response);
    }

    // 내 프로필 조회
    @Override
    @GetMapping("/me")
    public ApiResponse<MemberResDTO.MemberProfileDTO> getMemberProfile(
            @CurrentMember Member member
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND;
        MemberResDTO.MemberProfileDTO response = memberQueryService.findMemberProfile(member);
        return ApiResponse.onSuccess(code, response);
    }

    // 내 프로필 수정
    @Override
    @PatchMapping("/me")
    public ApiResponse<MemberResDTO.MemberProfileDTO> patchMember(
            @CurrentMember Member member,
            @RequestBody @Valid MemberReqDTO.UpdateMemberDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.UPDATED;
        MemberResDTO.MemberProfileDTO response = memberCommandService.updateMember(member, dto);
        return ApiResponse.onSuccess(code, response);
    }

    // 내 보유 기술 조회
    @Override
    @GetMapping("/me/techstacks")
    public ApiResponse<TechstackResDTO.DevTechstackListDTO> getMyTechstacks(
            @CurrentMember Member member
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND_TECHSTACK;
        TechstackResDTO.DevTechstackListDTO response = memberQueryService.findMemberTechstacks(member);
        return ApiResponse.onSuccess(code, response);
    }

    // 내 보유 기술 추가
    @Override
    @PostMapping("/me/techstacks")
    public ApiResponse<TechstackResDTO.DevTechstackListDTO> addMyTechstacks(
            @CurrentMember Member member,
            @RequestBody @Valid MemberReqDTO.AddTechstackDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.CREATED_TECHSTACK;
        TechstackResDTO.DevTechstackListDTO response = memberCommandService.addMemberTechstacks(member, dto);
        return ApiResponse.onSuccess(code, response);
    }

    // 내 보유 기술 삭제
    @Override
    @DeleteMapping("/me/techstacks")
    public ApiResponse<TechstackResDTO.DevTechstackListDTO> removeMyTechstacks(
            @CurrentMember Member member,
            @RequestBody @Valid MemberReqDTO.RemoveTechstackDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.DELETED_TECHSTACK;
        TechstackResDTO.DevTechstackListDTO response = memberCommandService.removeMemberTechstacks(member, dto);
        return ApiResponse.onSuccess(code, response);
    }

    // 내 깃허브 기록
    @Override
    @GetMapping("/me/contributions")
    public ApiResponse<MemberResDTO.ContributionListDTO> getContribution(
            @CurrentMember Member member,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND_CONTRIBUTIONS;
        MemberResDTO.ContributionListDTO response = memberQueryService.findMyContributions(member, from, to);
        return ApiResponse.onSuccess(code, response);
    }

    // 내 GitHub 레포지토리 목록 조회
    @Override
    @PostMapping("/me/git-repos")
    public ApiResponse<PagedResponse<MemberResDTO.GitRepoDTO>> syncGitRepos(
            @CurrentMember Member member,
            @ParameterObject @ModelAttribute @Valid MemberReqDTO.GitRepoSyncDTO dto
    ) {
        MemberSuccessCode code = MemberSuccessCode.FOUND_GIT_REPOS;
        PagedResponse<MemberResDTO.GitRepoDTO> response = memberCommandService.syncGitHubRepositories(member, dto);
        return ApiResponse.onSuccess(code, response);
    }
}
