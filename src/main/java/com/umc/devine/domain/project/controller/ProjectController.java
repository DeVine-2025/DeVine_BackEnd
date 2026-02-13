package com.umc.devine.domain.project.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.exception.code.ProjectSuccessCode;
import com.umc.devine.domain.project.service.command.ProjectCommandService;
import com.umc.devine.domain.project.service.query.ProjectQueryService;
import com.umc.devine.global.apiPayload.ApiResponse;
import com.umc.devine.global.security.CurrentMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
@Validated
public class ProjectController implements ProjectControllerDocs {

    private final ProjectCommandService projectCommandService;
    private final ProjectQueryService projectQueryService;

    @Override
    @PostMapping
    public ApiResponse<ProjectResDTO.CreateProjectRes> createProject(
            @CurrentMember Member member,
            @RequestBody @Valid ProjectReqDTO.CreateProjectReq request
    ) {
        ProjectSuccessCode code = ProjectSuccessCode.CREATED;
        ProjectResDTO.CreateProjectRes response = projectCommandService.createProject(member, request);
        return ApiResponse.onSuccess(code, response);
    }

    @Override
    @PatchMapping("/{projectId}")
    public ApiResponse<ProjectResDTO.UpdateProjectRes> updateProject(
            @CurrentMember Member member,
            @PathVariable("projectId") Long projectId,
            @RequestBody @Valid ProjectReqDTO.UpdateProjectReq request
    ) {
        ProjectSuccessCode code = ProjectSuccessCode.UPDATED;
        ProjectResDTO.UpdateProjectRes response = projectCommandService.updateProject(member, projectId, request);
        return ApiResponse.onSuccess(code, response);
    }

    @Override
    @PatchMapping("/{projectId}/status")
    public ApiResponse<Void> changeProjectStatus(
            @CurrentMember Member member,
            @PathVariable("projectId") Long projectId,
            @RequestBody @Valid ProjectReqDTO.ChangeStatusReq request
    ) {
        projectCommandService.changeProjectStatus(member, projectId, request.status());
        return ApiResponse.onSuccess(ProjectSuccessCode.STATUS_CHANGED, null);
    }

    @Override
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
            @CurrentMember Member member,
            @PathVariable("projectId") Long projectId
    ) {
        ProjectSuccessCode code = ProjectSuccessCode.DELETED;
        projectCommandService.deleteProject(member, projectId);
        return ApiResponse.onSuccess(code, null);
    }

    @Override
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResDTO.UpdateProjectRes> getProjectDetail(
            @PathVariable("projectId") Long projectId
    ) {
        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.UpdateProjectRes response = projectQueryService.getProjectDetail(projectId);
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

    // 프로젝트 검색 (프로젝트/개발자 보기 탭 하단)
    @Override
    @GetMapping
    public ApiResponse<ProjectResDTO.SearchProjectsRes> searchProjects(
            @ParameterObject @ModelAttribute @Valid ProjectReqDTO.SearchProjectReq request
    ) {
        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        return ApiResponse.onSuccess(code, projectQueryService.searchProjects(request));
    }

    // 추천 프로젝트 미리보기 (메인 하단 / 프로젝트·개발자 보기 탭 상단)
    @Override
    @GetMapping("/recommend/preview")
    public ApiResponse<ProjectResDTO.RecommendedProjectsRes> getRecommendedProjectsPreview(
            @CurrentMember Member member,
            @ParameterObject @ModelAttribute @Valid ProjectReqDTO.RecommendProjectsPreviewReq request
    ) {
        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.RecommendedProjectsRes response = projectQueryService.getRecommendedProjectsPreview(member, request);
        return ApiResponse.onSuccess(code, response);
    }

    // 추천 프로젝트 (추천 프로젝트 탭용 - 필터링, 상위 10개 고정 반환)
    @Override
    @GetMapping("/recommend")
    public ApiResponse<ProjectResDTO.RecommendedProjectsRes> getRecommendedProjects(
            @CurrentMember Member member,
            @ParameterObject @ModelAttribute @Valid ProjectReqDTO.RecommendProjectsReq request
    ) {
        ProjectSuccessCode code = ProjectSuccessCode.FOUND;
        ProjectResDTO.RecommendedProjectsRes response = projectQueryService.getRecommendedProjects(member, request);
        return ApiResponse.onSuccess(code, response);
    }

    // 내 프로젝트: 모집 중
    @Override
    @GetMapping("/my/recruiting")
    public ApiResponse<ProjectResDTO.MyProjectsRes> getMyRecruitingProjects(
            @CurrentMember Member member,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        ProjectResDTO.MyProjectsRes response = projectQueryService.getMyProjects(
                member, List.of(ProjectStatus.RECRUITING), pageable
        );
        return ApiResponse.onSuccess(ProjectSuccessCode.FOUND, response);
    }

    // 내 프로젝트: 진행 중
    @Override
    @GetMapping("/my/in-progress")
    public ApiResponse<ProjectResDTO.MyProjectsRes> getMyInProgressProjects(
            @CurrentMember Member member,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        ProjectResDTO.MyProjectsRes response = projectQueryService.getMyProjects(
                member, List.of(ProjectStatus.IN_PROGRESS), pageable
        );
        return ApiResponse.onSuccess(ProjectSuccessCode.FOUND, response);
    }

    // 내 프로젝트: 완료
    @Override
    @GetMapping("/my/completed")
    public ApiResponse<ProjectResDTO.MyProjectsRes> getMyCompletedProjects(
            @CurrentMember Member member,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        ProjectResDTO.MyProjectsRes response = projectQueryService.getMyProjects(
                member, List.of(ProjectStatus.COMPLETED), pageable
        );
        return ApiResponse.onSuccess(ProjectSuccessCode.FOUND, response);
    }
}
