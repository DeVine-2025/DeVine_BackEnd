package com.umc.devine.admin.member.controller;

import com.umc.devine.admin.member.dto.AdminMemberReqDTO;
import com.umc.devine.admin.member.dto.AdminMemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * TODO: 관리자 인증/인가 양식이 아직 확정되지 않아 관리자 권한 검증은 미적용. 양식 확정 후 추가 예정.
 */
@Tag(name = "Admin Member", description = "관리자 유저 관리 API")
public interface AdminMemberControllerDocs {

    @Operation(summary = "유저 검색/목록 조회", description = "이름/닉네임/이메일 검색어로 유저 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 목록 조회 성공")
    })
    ApiResponse<PagedResponse<AdminMemberResDTO.MemberSummaryDTO>> getMemberList(
            @ParameterObject @ModelAttribute @Valid AdminMemberReqDTO.SearchReq request
    );

    @Operation(summary = "유저 상세 조회", description = "프로필, 계정 상태, 결제 이력 요약, 로그인 이력을 함께 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "유저 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    ApiResponse<AdminMemberResDTO.MemberDetailRes> getMemberDetail(
            @Parameter(description = "닉네임", required = true) @PathVariable String nickname
    );

    @Operation(summary = "계정 상태 변경", description = "정지/정지해제/강제탈퇴/강제탈퇴취소 처리를 합니다. 강제탈퇴는 즉시 처리되지 않고 30일 소명 절차 후 확정됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "처리 사유 누락 또는 잘못된 상태 전이"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    ApiResponse<AdminMemberResDTO.ChangeStatusRes> changeStatus(
            @Parameter(hidden = true) @CurrentMember(required = false) Member member,
            @Parameter(description = "닉네임", required = true) @PathVariable String nickname,
            @RequestBody @Valid AdminMemberReqDTO.ChangeStatusReq request
    );
}
