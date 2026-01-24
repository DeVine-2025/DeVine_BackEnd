package com.umc.devine.domain.project.service.query;

import com.querydsl.core.types.Predicate;
import com.umc.devine.domain.project.converter.ProjectConverter;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.querydsl.ProjectPredicateBuilder;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static com.umc.devine.domain.project.exception.code.ProjectErrorCode.PROJECT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryServiceImpl implements ProjectQueryService {

    private static final int DEFAULT_PAGE_SIZE = 4;

    private final ProjectRepository projectRepository;
    private final ProjectRequirementTechstackRepository projectRequirementTechstackRepository;

    @Override
    @Transactional
    public ProjectResDTO.UpdateProjectRes getProjectDetail(Long memberId, Long projectId) {
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        // 원자적 조회수 증가 (동시성 안전)
        projectRepository.incrementViewCount(projectId);

        return ProjectConverter.toUpdateProjectRes(project, projectRequirementTechstackRepository);
    }

    @Override
    public ProjectResDTO.WeeklyBestProjectsRes getWeeklyBestProjects() {
        // 전체 프로젝트 중 주간 조회수 높은 순으로 조회
        // - 월요일 00:00:01에 스케줄러가 weeklyViewCount → previousWeekViewCount 이동
        // - 월요일: previousWeekViewCount 기준 (전주 완성 데이터, 초반 데이터 부족 방지)
        // - 화~일: weeklyViewCount 기준 (이번 주 월요일부터 쌓인 데이터)
        boolean isMonday = LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY;
        List<Project> projects = projectRepository.findWeeklyBestProjects(ProjectStatus.DELETED, isMonday);

        // 최대 4개만 반환
        List<ProjectResDTO.ProjectSummary> weeklyBestProjects = projects.stream()
                .limit(4)
                .map(project -> ProjectConverter.toProjectSummary(project, projectRequirementTechstackRepository))
                .toList();

        return ProjectResDTO.WeeklyBestProjectsRes.builder()
                .projects(weeklyBestProjects)
                .build();
    }

    @Override
    public ProjectResDTO.SearchProjectsRes searchProjects(ProjectReqDTO.SearchProjectReq request) {
        int pageIndex = request.page() - 1;
        Pageable pageable = PageRequest.of(pageIndex, DEFAULT_PAGE_SIZE);

        Predicate predicate = ProjectPredicateBuilder.buildSearchPredicate(request);
        Page<Project> projectPage = projectRepository.searchProjects(predicate, pageable);

        List<ProjectResDTO.ProjectSummary> summaries = projectPage.getContent().stream()
                .map(project -> ProjectConverter.toProjectSummary(project, projectRequirementTechstackRepository))
                .toList();

        PagedResponse<ProjectResDTO.ProjectSummary> pagedData = PagedResponse.of(projectPage, summaries);

        return ProjectResDTO.SearchProjectsRes.builder()
                .projects(pagedData)
                .build();
    }

    @Override
    public ProjectResDTO.RecommendedProjectsRes getRecommendedProjectsPreview(
            Long memberId,
            ProjectReqDTO.RecommendProjectsPreviewReq request
    ) {
        // TODO: 추천 알고리즘 기반 정렬 추가
        int limit = request.limit();

        // Preview는 필터링 없이 추천 점수 기준 상위 프로젝트만 반환
        List<Project> projects = projectRepository.findAllActiveProjects(limit);

        List<ProjectResDTO.RecommendedProjectSummary> summaries = projects.stream()
                .map(p -> ProjectConverter.toRecommendedProjectSummary(p, projectRequirementTechstackRepository))
                .toList();

        PagedResponse<ProjectResDTO.RecommendedProjectSummary> paged =
                previewToPagedResponse(summaries, limit);

        return ProjectResDTO.RecommendedProjectsRes.builder()
                .projects(paged)
                .build();
    }

    @Override
    public ProjectResDTO.RecommendedProjectsRes getRecommendedProjectsPage(
            Long memberId,
            ProjectReqDTO.RecommendProjectsPageReq request
    ) {
        // TODO: 추천 알고리즘 기반 정렬 추가
        int pageIndex = request.page() - 1;
        Pageable pageable = PageRequest.of(pageIndex, DEFAULT_PAGE_SIZE);

        Predicate predicate = ProjectPredicateBuilder.buildRecommendPagePredicate(request);
        Page<Project> projectPage = projectRepository.searchRecommendedProjects(predicate, pageable);

        List<ProjectResDTO.RecommendedProjectSummary> summaries = projectPage.getContent().stream()
                .map(project -> ProjectConverter.toRecommendedProjectSummary(project, projectRequirementTechstackRepository))
                .toList();

        PagedResponse<ProjectResDTO.RecommendedProjectSummary> pagedData = PagedResponse.of(projectPage, summaries);

        return ProjectResDTO.RecommendedProjectsRes.builder()
                .projects(pagedData)
                .build();
    }

    private PagedResponse<ProjectResDTO.RecommendedProjectSummary> previewToPagedResponse(
            List<ProjectResDTO.RecommendedProjectSummary> content,
            int size
    ) {
        // Page 객체를 만들기 어려워서 meta 직접 구성
        return PagedResponse.<ProjectResDTO.RecommendedProjectSummary>builder()
                .content(content)
                .page(1)
                .size(size)
                .totalElements(content.size())
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();
    }
}