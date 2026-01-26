package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;

public interface ProjectQueryService {
    // 프로젝트 상세 조회
    ProjectResDTO.UpdateProjectRes getProjectDetail(Long memberId, Long projectId);

    // 이번 주 주목 프로젝트 조회 (메인 화면 상단 - 4개)
    ProjectResDTO.WeeklyBestProjectsRes getWeeklyBestProjects();

    // 프로젝트 필터링 조회 (프로젝트/개발자 보기 탭 하단 - 4개씩 페이징, 필터링O)
    ProjectResDTO.SearchProjectsRes searchProjects(ProjectReqDTO.SearchProjectReq request);

    // 추천 프로젝트 미리보기 (메인 하단 / 프로젝트·개발자 보기 탭 상단)
    ProjectResDTO.RecommendedProjectsRes getRecommendedProjectsPreview(
            Long memberId,
            ProjectReqDTO.RecommendProjectsPreviewReq request
    );

    // 추천 프로젝트 페이지 (추천 프로젝트 탭용 - 필터링 + 페이징)
    ProjectResDTO.RecommendedProjectsRes getRecommendedProjectsPage(
            Long memberId,
            ProjectReqDTO.RecommendProjectsPageReq request
    );
}