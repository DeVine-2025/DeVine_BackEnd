package com.umc.devine.domain.report.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Report", description = "리포트 관련 API")
public interface ReportControllerDocs {

    @Operation(summary = "메인 리포트 조회", description = "Git 저장소의 메인 리포트를 조회합니다. PRIVATE 리포트는 소유자만 조회 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리포트에 대한 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트를 찾을 수 없음")
    })
    ApiResponse<ReportResDTO.ReportRes> getMainReport(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "Git 저장소 ID", required = true) @PathVariable Long gitRepoId
    );

    @Operation(summary = "상세 리포트 조회", description = "Git 저장소의 상세 리포트를 조회합니다. PRIVATE 리포트는 소유자만 조회 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리포트에 대한 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트를 찾을 수 없음")
    })
    ApiResponse<ReportResDTO.ReportRes> getDetailReport(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "Git 저장소 ID", required = true) @PathVariable Long gitRepoId
    );

    @Operation(summary = "리포트 공개 범위 수정", description = "리포트의 공개 범위를 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공개 범위 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리포트에 대한 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트를 찾을 수 없음")
    })
    ApiResponse<ReportResDTO.UpdateVisibilityRes> updateVisibility(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "리포트 ID", required = true) @PathVariable Long reportId,
            @RequestBody @Valid ReportReqDTO.UpdateVisibilityReq request
    );

    @Operation(summary = "리포트 생성 콜백", description = "FastAPI에서 리포트 생성 완료 후 호출하는 콜백 엔드포인트입니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "콜백 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트를 찾을 수 없음")
    })
    ApiResponse<Void> handleCallback(
            @RequestBody @Valid ReportReqDTO.CallbackReq request
    );

    @Operation(summary = "리포트 생성 요청", description = "Git 저장소에 대한 메인/상세 리포트 생성을 요청합니다. 비동기로 처리되며 즉시 202 Accepted를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "리포트 생성 요청 접수"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Git 저장소를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 리포트 생성이 진행 중")
    })
    ApiResponse<ReportResDTO.CreateReportRes> createReport(
            @Parameter(hidden = true) @CurrentMember Member member,
            @RequestBody @Valid ReportReqDTO.CreateReportReq request
    );

    @Operation(summary = "리포트 동기 생성", description = "Git 저장소에 대한 메인/상세 리포트를 동기적으로 생성합니다. 생성 완료까지 대기 후 결과를 반환합니다. (1-2분 소요)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "리포트 생성 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Git 저장소를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 리포트가 존재함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "리포트 생성 실패")
    })
    ApiResponse<ReportResDTO.CreateReportSyncRes> createReportSync(
            @Parameter(hidden = true) @CurrentMember Member member,
            @RequestBody @Valid ReportReqDTO.CreateReportReq request
    );

    @Operation(summary = "내 리포트 목록 조회", description = "현재 로그인한 사용자의 리포트 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포트 목록 조회 성공")
    })
    ApiResponse<ReportResDTO.ReportSummaryListDTO> getMyReports(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "리포트 타입 필터 (MAIN, DETAIL). 미지정 시 전체 조회") @RequestParam(required = false) ReportType type
    );

    @Operation(summary = "특정 회원 리포트 목록 조회", description = "닉네임으로 특정 회원의 공개 리포트 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리포트 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "프로필이 비공개 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음")
    })
    ApiResponse<ReportResDTO.ReportSummaryListDTO> getReportsByNickname(
            @Parameter(description = "조회할 회원의 닉네임", required = true) @PathVariable String nickname,
            @Parameter(description = "리포트 타입 필터 (MAIN, DETAIL). 미지정 시 전체 조회") @RequestParam(required = false) ReportType type
    );
}
