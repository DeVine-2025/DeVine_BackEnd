package com.umc.devine.domain.report.controller;

import com.umc.devine.domain.report.dto.ReportReqDTO;
import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.exception.code.ReportSuccessCode;
import com.umc.devine.domain.report.service.command.ReportCommandService;
import com.umc.devine.domain.report.service.query.ReportQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController implements ReportControllerDocs {

    private final ReportQueryService reportQueryService;
    private final ReportCommandService reportCommandService;

    @Override
    @GetMapping("/{gitRepoId}/main")
    public ApiResponse<ReportResDTO.ReportRes> getMainReport(@PathVariable Long gitRepoId) {
        ReportResDTO.ReportRes response = reportQueryService.getMainReport(gitRepoId);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_FOUND, response);
    }

    @Override
    @GetMapping("/{gitRepoId}/detail")
    public ApiResponse<ReportResDTO.ReportRes> getDetailReport(@PathVariable Long gitRepoId) {
        ReportResDTO.ReportRes response = reportQueryService.getDetailReport(gitRepoId);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_FOUND, response);
    }

    @Override
    @PatchMapping("/{reportId}/visibility")
    public ApiResponse<ReportResDTO.UpdateVisibilityRes> updateVisibility(
            @PathVariable Long reportId,
            @RequestBody @Valid ReportReqDTO.UpdateVisibilityReq request
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ReportResDTO.UpdateVisibilityRes response = reportCommandService.updateVisibility(memberId, reportId, request);
        return ApiResponse.onSuccess(ReportSuccessCode.VISIBILITY_UPDATED, response);
    }

    @Override
    @PostMapping
    public ApiResponse<ReportResDTO.CreateReportRes> createReport(
            @RequestBody @Valid ReportReqDTO.CreateReportReq request
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ReportResDTO.CreateReportRes response = reportCommandService.createReport(memberId, request);
        return ApiResponse.onSuccess(ReportSuccessCode.REPORT_GENERATION_REQUESTED, response);
    }
}
