package com.umc.devine.domain.report.controller;

import com.umc.devine.domain.report.dto.ReportResDTO;
import com.umc.devine.domain.report.exception.code.ReportSuccessCode;
import com.umc.devine.domain.report.service.query.ReportQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController implements ReportControllerDocs {

    private final ReportQueryService reportQueryService;

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
}
