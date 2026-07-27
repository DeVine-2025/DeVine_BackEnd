package com.umc.devine.admin.project.controller;

import com.umc.devine.admin.project.dto.AdminProjectReqDTO;
import com.umc.devine.admin.project.dto.AdminProjectResDTO;
import com.umc.devine.admin.project.exception.code.AdminProjectSuccessCode;
import com.umc.devine.admin.project.service.command.ProjectVisibilityCommandService;
import com.umc.devine.admin.project.service.query.AdminProjectQueryService;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.dto.PagedResponse;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/projects")
@Validated
public class AdminProjectController implements AdminProjectControllerDocs {

    private final ProjectVisibilityCommandService projectVisibilityCommandService;
    private final AdminProjectQueryService adminProjectQueryService;

    @Override
    @GetMapping
    public ApiResponse<PagedResponse<AdminProjectResDTO.ProjectSummaryDTO>> getProjectList(
            @ParameterObject @ModelAttribute @Valid AdminProjectReqDTO.SearchReq request
    ) {
        PagedResponse<AdminProjectResDTO.ProjectSummaryDTO> response = adminProjectQueryService.getProjectList(request);
        return ApiResponse.onSuccess(AdminProjectSuccessCode.PROJECT_LIST_FOUND, response);
    }

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
