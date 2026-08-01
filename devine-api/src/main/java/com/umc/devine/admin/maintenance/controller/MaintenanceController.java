package com.umc.devine.admin.maintenance.controller;

import com.umc.devine.admin.maintenance.dto.MaintenanceReqDTO;
import com.umc.devine.admin.maintenance.dto.MaintenanceResDTO;
import com.umc.devine.admin.maintenance.exception.code.MaintenanceSuccessCode;
import com.umc.devine.domain.maintenance.dto.MaintenanceState;
import com.umc.devine.domain.maintenance.service.MaintenanceModeService;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/maintenance")
public class MaintenanceController implements MaintenanceControllerDocs {

    private final MaintenanceModeService maintenanceModeService;

    @Override
    @GetMapping
    public ApiResponse<MaintenanceResDTO> getState() {
        MaintenanceState state = maintenanceModeService.getState();
        return ApiResponse.onSuccess(
                MaintenanceSuccessCode.MAINTENANCE_STATE_FOUND, MaintenanceResDTO.from(state));
    }

    @Override
    @PutMapping
    public ApiResponse<MaintenanceResDTO> update(@Valid @RequestBody MaintenanceReqDTO.UpdateDTO request) {
        MaintenanceState state = maintenanceModeService.update(
                request.enabled(), request.message(), request.estimatedEndAt());
        return ApiResponse.onSuccess(
                MaintenanceSuccessCode.MAINTENANCE_STATE_UPDATED, MaintenanceResDTO.from(state));
    }
}
