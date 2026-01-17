package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;

public interface ProjectQueryService {
    // 프로젝트 상세 조회
    ProjectResDTO.UpdateProjectRes getProjectDetail(Long memberId, Long projectId);

    // 이번 주 주목 프로젝트 조회 (메인 화면 상단 - 4개)
    ProjectResDTO.WeeklyBestProjectsRes getWeeklyBestProjects();

    //나에게 맞는 추천 프로젝트 조회 (메인 화면 하단 - 6개, 필터링X, 추천 알고리즘은 추후에)
    ProjectResDTO.ForMeProjectsRes getForMeProjects(Long memberId);

    // 추천 프로젝트 조회 (프로젝트/개발자 보기 탭 상단 - 4개, 필터링X, 추천 알고리즘은 추후에)
    ProjectResDTO.TopRecommendedProjectsRes getTopRecommendedProjects(Long memberId);

    // 프로젝트 필터링 조회 (프로젝트/개발자 보기 탭 하단 - 4개씩 페이징, 필터링O)
    ProjectResDTO.SearchProjectsRes searchProjects(ProjectReqDTO.SearchProjectReq request);

    // 추천 프로젝트 필터링 조회 (추천 프로젝트/개발자 탭 - 4개씩 페이징, 필터링O, 추천 알고리즘은 추후에)
    ProjectResDTO.SearchRecommendedProjectsRes searchRecommendedProjects(
            Long memberId,
            ProjectReqDTO.SearchRecommendedProjectReq request
    );
}