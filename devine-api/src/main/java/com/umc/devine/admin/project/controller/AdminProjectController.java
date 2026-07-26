package com.umc.devine.admin.project.controller;

import com.umc.devine.admin.project.dto.AdminProjectReqDTO;
import com.umc.devine.admin.project.dto.AdminProjectResDTO;
import com.umc.devine.admin.project.exception.code.AdminProjectSuccessCode;
import com.umc.devine.admin.project.service.command.ProjectVisibilityCommandService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/projects")
@Validated
public class AdminProjectController implements AdminProjectControllerDocs {

    private final ProjectVisibilityCommandService projectVisibilityCommandService;

    @Override
    @PatchMapping("/{projectId}/visibility")
    public ApiResponse<AdminProjectResDTO.UpdateVisibilityRes> updateVisibility(
            @CurrentMember(required = false) Member member,
            @PathVariable Long projectId,
            @RequestBody @Valid AdminProjectReqDTO.UpdateVisibilityReq request
    ) {
        Long processorMemberId = member != null ? member.getId() : null;
        AdminProjectResDTO.UpdateVisibilityRes response =
                projectVisibilityCommandService.changeVisibility(projectId, request.visible(), processorMemberId);
        return ApiResponse.onSuccess(AdminProjectSuccessCode.VISIBILITY_UPDATED, response);
    }
}
