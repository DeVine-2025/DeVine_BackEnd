package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.enums.ProjectStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectQueryService {
    // 프로젝트 상세 조회 (비회원 허용)
    /**
     * 프로젝트 상세 조회.
     * 비노출 처리된 프로젝트는 작성자 본인만 조회할 수 있으며, 그 외에는 존재하지 않는 것으로 처리된다.
     *
     * @param viewer 조회자. 비로그인 조회를 허용하는 엔드포인트라 null일 수 있다.
     */
    ProjectResDTO.UpdateProjectRes getProjectDetail(Member viewer, Long projectId);

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
    /**
     * 작성자 본인의 "내 프로젝트" 목록. 소유자가 자기 글의 비노출 여부를 확인할 수 있도록
     * 비노출 프로젝트도 포함하며, 응답의 {@code visible}로 구분한다.
     *
     * <p>제3자에게 노출되는 경로에서는 {@link #getPublicProjectsOf}를 사용해야 한다.
     */
    ProjectResDTO.MyProjectsRes getMyProjects(Member member, List<ProjectStatus> statuses, Pageable pageable);

    /**
     * 공개 프로필에서 보는 특정 회원의 프로젝트 목록. 비노출 프로젝트는 제외된다.
     *
     * @param owner 프로필 주인. 조회자가 아니다.
     */
    ProjectResDTO.MyProjectsRes getPublicProjectsOf(Member owner, List<ProjectStatus> statuses, Pageable pageable);

    // 내가 생성한 모집 중인 프로젝트 목록 조회 (매칭 수락된 프로젝트 제외, 개발자 추천 필터용)
    ProjectResDTO.MyProjectsRes getMyCreatedRecruitingProjects(Member member, Pageable pageable);
}