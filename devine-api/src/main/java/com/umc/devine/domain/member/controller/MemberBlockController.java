package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberBlockResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.exception.code.MemberSuccessCode;
import com.umc.devine.domain.member.service.MemberBlockService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.CurrentMember;
import com.umc.devine.global.validation.annotation.ValidNickname;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/member-blocks")
@RequiredArgsConstructor
@Validated
@Tag(name = "회원 차단")
public class MemberBlockController {
    private final MemberBlockService service;

    @GetMapping("/{nickname}/confirmation")
    @Operation(summary = "차단 확인: 취소될 대기 건 수와 안내")
    public ApiResponse<MemberBlockResDTO.Confirmation> confirmation(
            @CurrentMember Member member, @PathVariable @ValidNickname String nickname) {
        return ApiResponse.onSuccess(MemberSuccessCode.FOUND, service.confirmation(member, nickname));
    }

    @PostMapping("/{nickname}")
    @Operation(summary = "회원 차단 (이미 차단한 경우에도 성공)")
    public ApiResponse<Void> block(@CurrentMember Member member, @PathVariable @ValidNickname String nickname) {
        service.block(member, nickname);
        return ApiResponse.onSuccess(MemberSuccessCode.BLOCKED, null);
    }

    @GetMapping
    @Operation(summary = "내가 차단한 회원 목록 (최근 순)")
    public ApiResponse<PagedResponse<MemberBlockResDTO.BlockedMember>> list(
            @CurrentMember Member member,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return ApiResponse.onSuccess(MemberSuccessCode.FOUND, service.list(member, PageRequest.of(page - 1, size)));
    }

    @DeleteMapping("/{nickname}")
    @Operation(summary = "회원 차단 해제 (이미 해제한 경우에도 성공)")
    public ApiResponse<Void> unblock(@CurrentMember Member member, @PathVariable @ValidNickname String nickname) {
        service.unblock(member, nickname);
        return ApiResponse.onSuccess(MemberSuccessCode.UNBLOCKED, null);
    }
}
