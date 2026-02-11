package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.matching.MatchingReqDTO;
import com.umc.devine.domain.project.dto.matching.MatchingResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import com.umc.devine.global.validation.annotation.ValidNickname;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Matching", description = "프로젝트 매칭 관련 API")
public interface MatchingControllerDocs {

    @Operation(summary = "프로젝트 지원하기", description = "개발자가 PM의 프로젝트에 지원합니다. 지원할 파트를 선택해야 합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "프로젝트 지원 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (모집 중이 아닌 프로젝트, 본인 프로젝트 지원 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "개발자만 지원 가능"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 회원을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 지원한 프로젝트")
    })
    ApiResponse<MatchingResDTO.ProposeResDTO> applyToProject(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Valid @RequestBody MatchingReqDTO.ApplyReqDTO dto
    );

    @Operation(summary = "프로젝트 지원 파트 수정", description = "개발자가 지원한 프로젝트의 지원 파트를 수정합니다. PENDING 상태에서만 변경 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지원 파트 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "PENDING 상태가 아닌 매칭"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매칭 정보를 찾을 수 없음")
    })
    ApiResponse<MatchingResDTO.ProposeResDTO> updateApplication(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Valid @RequestBody MatchingReqDTO.ApplyReqDTO dto
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
            @Valid @RequestBody MatchingReqDTO.DecisionReqDTO dto
    );

    @Operation(summary = "프로젝트 제안하기", description = "PM이 개발자에게 프로젝트를 제안합니다. 제안할 파트를 선택해야 합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "프로젝트 제안 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (모집 중이 아닌 프로젝트, 대상이 개발자가 아님 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PM만 제안 가능"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 또는 회원을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 제안한 회원")
    })
    ApiResponse<MatchingResDTO.ProposeResDTO> proposeToMember(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "개발자 닉네임", required = true) @PathVariable @ValidNickname String nickname,
            @Valid @RequestBody MatchingReqDTO.ProposeReqDTO dto
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
            @Valid @RequestBody MatchingReqDTO.DecisionReqDTO dto
    );

    @Operation(summary = "PM - 제안한 개발자 목록", description = "PM이 프로젝트에 제안한 개발자 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PM만 조회 가능")
    })
    ApiResponse<MatchingResDTO.DevelopersRes> getProposedDevelopers(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(summary = "PM - 개발자 지원 현황", description = "PM의 프로젝트에 지원한 개발자 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PM만 조회 가능")
    })
    ApiResponse<MatchingResDTO.DevelopersRes> getPmApplications(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(summary = "개발자 - 받은 제안 목록", description = "개발자가 PM으로부터 받은 프로젝트 제안 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "개발자만 조회 가능")
    })
    ApiResponse<MatchingResDTO.ProjectsRes> getReceivedProposals(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(summary = "개발자 - 지원 중 목록", description = "개발자가 지원한 프로젝트 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "개발자만 조회 가능")
    })
    ApiResponse<MatchingResDTO.ProjectsRes> getDeveloperApplications(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(hidden = true) Pageable pageable
    );


    @Operation(summary = "개발자 - 내 지원 상태 조회",
            description = "특정 프로젝트에 대한 본인의 지원 상태를 조회합니다. (APPLY 타입만) exists=false이면 지원하지 않은 상태입니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "지원 상태 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트를 찾을 수 없음"
            )
    })
    ApiResponse<MatchingResDTO.MatchingStatusRes> getMyApplyStatus(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId
    );

    @Operation(summary = "PM - 제안 상태 조회",
            description = "PM이 특정 개발자에게 제안한 상태를 조회합니다. (PROPOSE 타입만) exists=false이면 제안하지 않은 상태입니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "제안 상태 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "본인 프로젝트만 조회 가능"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트 또는 회원을 찾을 수 없음"
            )
    })
    ApiResponse<MatchingResDTO.MatchingStatusRes> getMyProposeStatus(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "프로젝트 ID", required = true) @PathVariable Long projectId,
            @Parameter(description = "대상 회원 닉네임", required = true) @PathVariable @ValidNickname String nickname
    );
}
