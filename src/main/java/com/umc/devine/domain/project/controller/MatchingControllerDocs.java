package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.matching.MatchingReqDTO;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Matching", description = "프로젝트 매칭 관련 API")
public interface MatchingControllerDocs {

    @Operation(summary = "프로젝트 지원하기", description = "개발자가 PM의 프로젝트에 지원합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "프로젝트 지원 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (모집 중이 아닌 프로젝트, 본인 프로젝트 지원 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "개발자만 지원 가능"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 회원을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 지원한 프로젝트")
    })
    ApiResponse<MatchingResDTO.ProposeResDTO> applyToProject(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId
    );

    @Operation(summary = "프로젝트 지원 취소하기", description = "개발자가 지원했던 프로젝트에 대해 지원을 취소합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 지원 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매칭 정보를 찾을 수 없음")
    })
    ApiResponse<MatchingResDTO.ProposeResDTO> cancelApplication(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId
    );

    @Operation(summary = "프로젝트 제안하기", description = "PM이 개발자에게 프로젝트를 제안합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "프로젝트 제안 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (모집 중이 아닌 프로젝트, 대상이 개발자가 아님 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PM만 제안 가능"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 회원을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 제안한 회원")
    })
    ApiResponse<MatchingResDTO.ProposeResDTO> proposeToMember(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "개발자 닉네임", required = true) @PathVariable String nickname,
            @RequestBody MatchingReqDTO.ProposeReqDTO dto
    );

    @Operation(summary = "지원 수락/거절", description = "PM이 자신의 프로젝트에 온 지원을 수락하거나 거절합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지원 응답 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 처리된 매칭, 잘못된 매칭 타입 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PM만 응답 가능 또는 본인 프로젝트가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매칭 정보를 찾을 수 없음")
    })
    ApiResponse<MatchingResDTO.ProposeResDTO> respondToApplication(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "매칭 ID", required = true) @PathVariable Long matchingId,
            @RequestBody MatchingReqDTO.DecisionReqDTO dto
    );

    @Operation(summary = "제안 수락/거절", description = "개발자가 PM으로부터 받은 프로젝트 제안을 수락하거나 거절합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제안 응답 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 처리된 매칭, 잘못된 매칭 타입 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "개발자만 응답 가능 또는 본인에게 온 제안이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매칭 정보를 찾을 수 없음")
    })
    ApiResponse<MatchingResDTO.ProposeResDTO> respondToProposal(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "매칭 ID", required = true) @PathVariable Long matchingId,
            @RequestBody MatchingReqDTO.DecisionReqDTO dto
    );
}
