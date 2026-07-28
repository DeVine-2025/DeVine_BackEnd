package com.umc.devine.admin.dashboard.controller;

import com.umc.devine.admin.dashboard.dto.AdminDashboardResDTO;
import com.umc.devine.admin.dashboard.exception.code.AdminDashboardSuccessCode;
import com.umc.devine.admin.dashboard.service.query.AdminDashboardQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/dashboard")
public class AdminDashboardController implements AdminDashboardControllerDocs {

    private final AdminDashboardQueryService adminDashboardQueryService;

    @Override
    @GetMapping
    public ApiResponse<AdminDashboardResDTO.DashboardDTO> getDashboard() {
        return ApiResponse.onSuccess(
                AdminDashboardSuccessCode.DASHBOARD_FOUND,
                adminDashboardQueryService.getDashboard()
        );
    }
}
