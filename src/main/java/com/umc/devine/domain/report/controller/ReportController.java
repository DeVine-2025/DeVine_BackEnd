package com.umc.devine.domain.report.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.exception.code.ReportSuccessCode;
import com.umc.devine.domain.report.service.command.ReportCommandService;
import com.umc.devine.domain.report.service.query.ReportQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import com.umc.devine.global.validation.annotation.ValidNickname;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
@Validated
public class ReportController implements ReportControllerDocs {

    private final ReportQueryService reportQueryService;
    private final ReportCommandService reportCommandService;

    @Override
    @GetMapping("/{gitRepoId}/main")
    public ApiResponse<ReportResDTO.ReportRes> getMainReport(
            @CurrentMember Member member,
            @PathVariable Long gitRepoId
    ) {
        ReportResDTO.ReportRes response = reportQueryService.getMainReport(member.getId(), gitRepoId);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_FOUND, response);
    }

    @Override
    @GetMapping("/{gitRepoId}/detail")
    public ApiResponse<ReportResDTO.ReportRes> getDetailReport(
            @CurrentMember Member member,
            @PathVariable Long gitRepoId
    ) {
        ReportResDTO.ReportRes response = reportQueryService.getDetailReport(member.getId(), gitRepoId);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_FOUND, response);
    }

    @Override
    @PatchMapping("/{reportId}/visibility")
    public ApiResponse<ReportResDTO.UpdateVisibilityRes> updateVisibility(
            @CurrentMember Member member,
            @PathVariable Long reportId,
            @RequestBody @Valid ReportReqDTO.UpdateVisibilityReq request
    ) {
        ReportResDTO.UpdateVisibilityRes response = reportCommandService.updateVisibility(member.getId(), reportId, request);
        return ApiResponse.onSuccess(ReportSuccessCode.VISIBILITY_UPDATED, response);
    }

    @Override
    @PostMapping("/callback")
    public ApiResponse<Void> handleCallback(
            @RequestBody @Valid ReportReqDTO.CallbackReq request
    ) {
        reportCommandService.processCallback(request);
        return ApiResponse.onSuccess(ReportSuccessCode.CALLBACK_PROCESSED, null);
    }

    @Override
    @PostMapping
    public ApiResponse<ReportResDTO.CreateReportRes> createReport(
            @CurrentMember Member member,
            @RequestBody @Valid ReportReqDTO.CreateReportReq request
    ) {
        ReportResDTO.CreateReportRes response = reportCommandService.createReport(member.getId(), request);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_GENERATION_REQUESTED, response);
    }

    @Override
    @PostMapping("/sync")
    public ApiResponse<ReportResDTO.CreateReportSyncRes> createReportSync(
            @CurrentMember Member member,
            @RequestBody @Valid ReportReqDTO.CreateReportReq request
    ) {
        ReportResDTO.CreateReportSyncRes response = reportCommandService.createReportSync(member.getId(), request);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_CREATED, response);
    }

    @Override
    @GetMapping("/me")
    public ApiResponse<ReportResDTO.ReportSummaryListDTO> getMyReports(
            @CurrentMember Member member,
            @RequestParam(required = false) ReportType type
    ) {
        ReportResDTO.ReportSummaryListDTO response = reportQueryService.getMyReports(member, type);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_FOUND, response);
    }

    @Override
    @GetMapping("/members/{nickname}")
    public ApiResponse<ReportResDTO.ReportSummaryListDTO> getReportsByNickname(
            @PathVariable @ValidNickname String nickname,
            @RequestParam(required = false) ReportType type
    ) {
        ReportResDTO.ReportSummaryListDTO response = reportQueryService.getReportsByNickname(nickname, type);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_FOUND, response);
    }
}
