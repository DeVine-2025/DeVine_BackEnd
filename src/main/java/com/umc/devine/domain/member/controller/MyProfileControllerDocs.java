package com.umc.devine.domain.member.controller;

import com.umc.devine.domain.member.dto.MemberReqDTO;
import com.umc.devine.domain.member.dto.MemberResDTO;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.techstack.dto.DevReportResDTO;
import com.umc.devine.domain.techstack.dto.TechstackResDTO;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.auth.ClerkPrincipal;
import com.umc.devine.global.auth.CurrentMember;
import com.umc.devine.global.validation.annotation.ValidNickname;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

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

    @Operation(summary = "내 깃허브 기록 조회 API", description = "내 깃허브 기록을 조회하는 API입니다. 현재는 하드코딩된 ID(1L)를 사용하고 있고, 외부 api 호출하지 않은 상태입니다. 반환 값은 Mock 데이터 입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<MemberResDTO.ContributionListDTO> getContribution();

    @Operation(summary = "내 프로젝트 조회 API", description = "내 프로젝트 목록을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<ProjectResDTO.ProjectListDTO> getProjects(
            @Parameter(hidden = true) @CurrentMember Member member
    );

    @Operation(summary = "내 리포트 조회 API", description = "내가 가진 개발 리포트 목록을 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한이 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 회원을 찾을 수 없습니다.")
    })
    ApiResponse<DevReportResDTO.ReportListDTO> getMyReports(
            @Parameter(hidden = true) @CurrentMember Member member
    );
}
