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

    // 이번 주 주목 프로젝트 조회 (메인 화면 상단 - 4개)
    @Override
    @GetMapping("/weekly-best")
    public ApiResponse<ProjectResDTO.WeeklyBestProjectsRes> getWeeklyBestProjects() {
        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.WeeklyBestProjectsRes response = projectQueryService.getWeeklyBestProjects();
        return ApiResponse.onSuccess(code, response);
    }

    // 나에게 맞는 추천 프로젝트 조회 (메인 화면 하단)
    @Override
    @GetMapping("/for-me")
    public ApiResponse<ProjectResDTO.ForMeProjectsRes> getForMeProjects() {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.ForMeProjectsRes response = projectQueryService.getForMeProjects(memberId);
        return ApiResponse.onSuccess(code, response);
    }

    // 상단 추천 프로젝트 조회 (프로젝트/개발자 보기 탭 상단)
    @Override
    @GetMapping("/top-recommendations")
    public ApiResponse<ProjectResDTO.TopRecommendedProjectsRes> getTopRecommendedProjects() {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.TopRecommendedProjectsRes response = projectQueryService.getTopRecommendedProjects(memberId);
        return ApiResponse.onSuccess(code, response);
    }

    // 프로젝트 검색 (프로젝트/개발자 보기 탭 하단)
    @Override
    @PostMapping("/search")
    public ApiResponse<ProjectResDTO.SearchProjectsRes> searchProjects(
            @RequestBody @Valid ProjectReqDTO.SearchProjectReq request
    ) {
        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.SearchProjectsRes response = projectQueryService.searchProjects(request);
        return ApiResponse.onSuccess(code, response);
    }

    // 추천 프로젝트 검색 (추천 프로젝트/개발자 탭)
    @Override
    @PostMapping("/recommendations/search")
    public ApiResponse<ProjectResDTO.SearchRecommendedProjectsRes> searchRecommendedProjects(
            @RequestBody @Valid ProjectReqDTO.SearchRecommendedProjectReq request
    ) {
        // TODO: 토큰 방식으로 변경
        Long memberId = 1L;

        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.SearchRecommendedProjectsRes response =
                projectQueryService.searchRecommendedProjects(memberId, request);
        return ApiResponse.onSuccess(code, response);
    }
}