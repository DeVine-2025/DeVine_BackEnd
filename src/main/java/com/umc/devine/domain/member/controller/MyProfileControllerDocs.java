package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.global.security.CurrentMember;
import com.umc.devine.global.validation.annotation.ValidNickname;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Tag(name = "Member", description = "회원가입, 내 정보 관련 API")
public interface MyProfileControllerDocs {

    @Operation(summary = "이용약관 조회 API", description = "회원가입 시 필요한 이용약관 목록을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<MemberResDTO.TermsListDTO> getTerms();

    @Operation(summary = "회원가입 API", description = "소셜 로그인 후 추가 정보를 입력하여 회원가입을 완료하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 약관 미동의, 유효성 검증 실패 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카테고리 또는 기술 스택을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 회원입니다.")
    })
    ApiResponse<MemberResDTO.SignupResultDTO> signup(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @RequestBody @Valid MemberReqDTO.SignupDTO dto
    );

    @Operation(summary = "닉네임 중복 체크 API", description = "닉네임 중복 여부를 확인하는 API입니다. isDuplicate가 true이면 이미 사용 중인 닉네임입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공")
    })
    ApiResponse<MemberResDTO.NicknameDuplicateDTO> checkNicknameDuplicate(@RequestParam("nickname") @ValidNickname String nickname);

    @Operation(summary = "내 프로필 조회 API", description = "내 프로필 정보를 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<MemberResDTO.MemberProfileDTO> getMemberProfile(
            @Parameter(hidden = true) @CurrentMember Member member
    );

    @Operation(summary = "내 프로필 수정 API", description = "내 프로필 정보를 수정하는 API입니다. 닉네임, 프로필 이미지 URL, 주소, 자기소개, 도메인들, 연락처들, 메인타입, 공개여부를 수정할 수 있습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<MemberResDTO.MemberProfileDTO> patchMember(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Valid @RequestBody MemberReqDTO.UpdateMemberDTO dto
    );

    @Operation(summary = "내 보유 기술 조회 API", description = "내가 보유한 기술 스택 목록을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<TechstackResDTO.DevTechstackListDTO> getMyTechstacks(
            @Parameter(hidden = true) @CurrentMember Member member
    );

    @Operation(summary = "내 보유 기술 추가 API", description = "내가 보유할 기술 추가하는 API입니다. techstackId를 입력 받아 추가합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원 또는 기술 스택을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 보유한 기술 스택입니다.")
    })
    ApiResponse<TechstackResDTO.DevTechstackListDTO> addMyTechstacks(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Valid @RequestBody MemberReqDTO.AddTechstackDTO dto
    );

    @Operation(summary = "내 보유 기술 삭제 API", description = "내가 보유한 기술을 삭제하는 API입니다. techstackId 목록을 입력 받아 삭제합니다. source(AUTO, MANUAL)를 지정하면 해당 출처의 기술만 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원 또는 기술 스택을 찾을 수 없습니다.")
    })
    ApiResponse<TechstackResDTO.DevTechstackListDTO> removeMyTechstacks(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Valid @RequestBody MemberReqDTO.RemoveTechstackDTO dto
    );

    @Operation(summary = "내 깃허브 기록 조회 API", description = "내 깃허브 기록(잔디)을 조회하는 API입니다. GitHub GraphQL API를 통해 기여 데이터를 가져옵니다. from/to 파라미터로 조회 기간을 지정할 수 있습니다. (ISO 8601 형식, 예: 2024-01-01T00:00:00Z)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "GitHub 연동이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<MemberResDTO.ContributionListDTO> getContribution(
            @Parameter(hidden = true) @CurrentMember Member member,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    );

    @Operation(summary = "내 GitHub 레포지토리 목록 조회 API", description = "내 GitHub 계정에 연결된 레포지토리 목록을 동기화하고 페이지네이션된 결과를 조회합니다. 내가 소유한 레포와 기여한 레포(커밋/PR)만 가져옵니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "GitHub 연동이 필요합니다.")
    })
    ApiResponse<PagedResponse<MemberResDTO.GitRepoDTO>> syncGitRepos(
            @Parameter(hidden = true) @CurrentMember Member member,
            @ParameterObject @ModelAttribute @Valid MemberReqDTO.GitRepoSyncDTO dto
    );
}
