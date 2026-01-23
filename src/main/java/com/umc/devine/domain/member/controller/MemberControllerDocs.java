package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.ClerkPrincipal;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;

@Tag(name = "Members", description = "회원 관련 API")
public interface MemberControllerDocs {

    @Operation(summary = "닉네임 중복 체크 API", description = "닉네임 중복 여부를 확인하는 API입니다. isDuplicate가 true이면 이미 사용 중인 닉네임입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<MemberResDTO.NicknameDuplicateDTO> checkNicknameDuplicate(String nickname);

    @Operation(summary = "프로젝트에 맞는 개발자 추천 API (페이지네이션 포함)", description = "프로젝트에 맞는 개발자를 추천하는 API입니다. projectIds, category(도메인), techGenre, techstackName으로 필터링할 수 있습니다. 모든 파라미터는 선택적입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<PagedResponse<MemberResDTO.DeveloperDTO>> getRecommendDevelopers(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @ParameterObject @ModelAttribute MemberReqDTO.RecommendDeveloperDTO dto
    );

    // TODO : 나에게 맞는 개발자 추천 (프리뷰)

    @Operation(summary = "개발자 검색 API (페이지네이션 포함)", description = "조건에 따라 개발자를 검색하는 API입니다. category, techGenre, techstackName 파라미터로 필터링할 수 있습니다. 모든 파라미터는 선택적이며, 지정하지 않으면 해당 조건은 무시됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다.")
    })
    ApiResponse<PagedResponse<MemberResDTO.UserProfileDTO>> searchDevelopers(
            @ParameterObject @ModelAttribute MemberReqDTO.SearchDeveloperDTO dto
    );

    @Operation(summary = "특정 회원 프로필 조회 API", description = "특정 회원 프로필 정보를 조회하는 API입니다. 닉네임(nickname)을 path variable로 전달해주세요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<MemberResDTO.UserProfileDTO> getMemberByNickname(String nickname);

    @Operation(summary = "특정 회원 깃허브 기록 조회 API", description = "특정 회원의 깃허브 기록을 조회하는 API입니다. 닉네임(nickname)을 path variable로 전달해주세요. 외부 api 호출하지 않은 상태입니다. 반환 값은 Mock 데이터 입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<MemberResDTO.ContributionListDTO> getContributionByNickname(String nickname);

    @Operation(summary = "특정 회원 리포트 조회 API", description = "특정 회원의 개발 리포트 목록을 조회하는 API입니다. 닉네임(nickname)을 path variable로 전달해주세요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<DevReportResDTO.ReportListDTO> getReportsByNickname(String nickname);
}
