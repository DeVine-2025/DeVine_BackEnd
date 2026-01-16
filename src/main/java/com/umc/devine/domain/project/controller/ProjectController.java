package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.exception.code.ProjectSuccessCode;
import com.umc.devine.domain.project.service.command.ProjectCommandService;
import com.umc.devine.domain.project.service.query.ProjectQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController implements ProjectControllerDocs {

    private final ProjectCommandService projectCommandService;
    private final ProjectQueryService projectQueryService;

    @Override
    @PostMapping
    public ApiResponse<ProjectResDTO.CreateProjectRes> createProject(
            @RequestBody @Valid ProjectReqDTO.CreateProjectReq request
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ProjectSuccessCode code = ProjectSuccessCode.CREATED;
        ProjectResDTO.CreateProjectRes response = projectCommandService.createProject(memberId, request);
        return ApiResponse.onSuccess(code, response);
    }

    @Override
    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectResDTO.UpdateProjectRes> updateProject(
            @PathVariable("projectId") Long projectId,
            @RequestBody @Valid ProjectReqDTO.UpdateProjectReq request
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ProjectSuccessCode code = ProjectSuccessCode.UPDATED;
        ProjectResDTO.UpdateProjectRes response = projectCommandService.updateProject(memberId, projectId, request);
        return ApiResponse.onSuccess(code, response);
    }

    @Override
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
            @PathVariable("projectId") Long projectId
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ProjectSuccessCode code = ProjectSuccessCode.DELETED;
        projectCommandService.deleteProject(memberId, projectId);
        return ApiResponse.onSuccess(code, null);
    }

    @Override
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResDTO.UpdateProjectRes> getProjectDetail(
            @PathVariable("projectId") Long projectId
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.UpdateProjectRes response = projectQueryService.getProjectDetail(memberId, projectId);
        return ApiResponse.onSuccess(code, response);
    }
}