package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.CurrentMember;
import com.umc.devine.global.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import com.umc.devine.global.validation.annotation.ValidNickname;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Members", description = "특정 회원, 전체 회원 조회 관련 API")
public interface MemberControllerDocs {

    @Operation(summary = "프로젝트에 맞는 개발자 추천 API (페이지네이션 포함)", description = "프로젝트에 맞는 개발자를 추천하는 API입니다. projectId를 기반으로 해당 프로젝트의 카테고리에 맞는 개발자를 추천합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<PagedResponse<MemberResDTO.DeveloperDTO>> getRecommendDevelopers(
            @Parameter(hidden = true) @CurrentMember Member member,
            @ParameterObject @ModelAttribute @Valid MemberReqDTO.RecommendDeveloperDTO dto
    );

    @Operation(summary = "개발자 추천 프리뷰 API", description = "나에게 맞는 개발자 추천 프리뷰입니다. limit 파라미터로 조회할 개수를 지정할 수 있습니다. (기본값: 4)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다.")
    })
    ApiResponse<List<MemberResDTO.DeveloperDTO>> getRecommendDevelopersPreview(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
            @Max(value = 20, message = "limit은 20 이하여야 합니다.")
            int limit
    );

    @Operation(summary = "개발자 검색 API (페이지네이션 포함)", description = "조건에 따라 개발자를 검색하는 API입니다. categories(도메인 목록), techstackNames(기술스택 목록) 파라미터로 필터링할 수 있습니다. 복수 선택 가능하며, 모든 파라미터는 선택적이고 지정하지 않으면 해당 조건은 무시됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다.")
    })
    ApiResponse<PagedResponse<MemberResDTO.UserProfileDTO>> searchDevelopers(
            @ParameterObject @ModelAttribute @Valid MemberReqDTO.SearchDeveloperDTO dto
    );

    @Operation(summary = "특정 회원 프로필 조회 API", description = "특정 회원 프로필 정보를 조회하는 API입니다. 닉네임(nickname)을 path variable로 전달해주세요.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<MemberResDTO.UserProfileDTO> getMemberByNickname(@ValidNickname String nickname);

    @Operation(summary = "특정 회원 깃허브 기록 조회 API", description = "특정 회원의 깃허브 기록(잔디)을 조회하는 API입니다. 닉네임(nickname)을 path variable로 전달해주세요. 해당 회원의 GitHub username이 등록되어 있어야 조회 가능합니다. from/to 파라미터로 조회 기간을 지정할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "비공개 프로필입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<MemberResDTO.ContributionListDTO> getContributionByNickname(
            @ValidNickname String nickname,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    );
}
