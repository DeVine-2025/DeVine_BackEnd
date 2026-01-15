package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.command.MemberCommandService;
import com.umc.devine.domain.member.service.query.MemberQueryService;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
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
}
