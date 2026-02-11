package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.enums.ProjectStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectQueryService {
    // 프로젝트 상세 조회 (비회원 허용)
    ProjectResDTO.UpdateProjectRes getProjectDetail(Long projectId);

    // 이번 주 주목 프로젝트 조회 (메인 화면 상단 - 4개)
    ProjectResDTO.WeeklyBestProjectsRes getWeeklyBestProjects();

    // 프로젝트 필터링 조회 (프로젝트/개발자 보기 탭 하단 - 4개씩 페이징, 필터링O)
    ProjectResDTO.SearchProjectsRes searchProjects(ProjectReqDTO.SearchProjectReq request);

    // 추천 프로젝트 미리보기 (메인 하단 / 프로젝트·개발자 보기 탭 상단)
    ProjectResDTO.RecommendedProjectsRes getRecommendedProjectsPreview(
            Member member,
            ProjectReqDTO.RecommendProjectsPreviewReq request
    );

    // 추천 프로젝트 (추천 프로젝트 탭용 - 필터링, 상위 10개 고정 반환)
    ProjectResDTO.RecommendedProjectsRes getRecommendedProjects(
            Member member,
            ProjectReqDTO.RecommendProjectsReq request
    );

    // 내 프로젝트 목록 조회 (상태별)
    ProjectResDTO.MyProjectsRes getMyProjects(Member member, List<ProjectStatus> statuses, Pageable pageable);
}