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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static com.umc.devine.domain.project.exception.code.ProjectErrorCode.PROJECT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryServiceImpl implements ProjectQueryService {

    private final ProjectRepository projectRepository;
    private final ProjectRequirementTechstackRepository projectRequirementTechstackRepository;

    @Override
    @Transactional
    public ProjectResDTO.UpdateProjectRes getProjectDetail(Long memberId, Long projectId) {
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        project.incrementViewCount();

        return ProjectConverter.toUpdateProjectRes(project, projectRequirementTechstackRepository);
    }

    @Override
    public ProjectResDTO.WeeklyBestProjectsRes getWeeklyBestProjects() {
        // 지난 주 월요일 00:00:00 (조회수 집계 기준 시작)
        LocalDateTime lastMonday = LocalDateTime.now()
                .with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        // 지난 주 일요일 23:59:59 (조회수 집계 기준 종료)
        LocalDateTime lastSunday = lastMonday
                .plusDays(6)
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999999999);

        // 현재 요일 확인
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        LocalDateTime startOfWeek;
        LocalDateTime endOfWeek;

        if (today == DayOfWeek.MONDAY) {
            // 월요일: 이번 주 월요일~일요일 생성된 프로젝트
            startOfWeek = LocalDateTime.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            endOfWeek = startOfWeek
                    .plusDays(6)
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999999999);
        } else {
            // 화~일요일: 지난 주 월요일~일요일 생성된 프로젝트
            startOfWeek = lastMonday;
            endOfWeek = lastSunday;
        }

        // 해당 기간에 생성된 프로젝트 중 주간 조회수 높은 순으로 조회
        List<Project> projects = projectRepository.findWeeklyBestProjects(
                ProjectStatus.DELETED,
                startOfWeek,
                endOfWeek
        );

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
    public ProjectResDTO.ForMeProjectsRes getForMeProjects(Long memberId) {
        // TODO: 실제 추천 알고리즘 구현
        // 현재는 최신 생성된 프로젝트 6개 반환
        List<Project> projects = projectRepository.findAllActiveProjects(6);

        List<ProjectResDTO.RecommendedProjectSummary> forMeProjects = projects.stream()
                .map(project -> ProjectConverter.toRecommendedProjectSummary(project, projectRequirementTechstackRepository))
                .toList();

        return ProjectResDTO.ForMeProjectsRes.builder()
                .projects(forMeProjects)
                .build();
    }

    @Override
    public ProjectResDTO.TopRecommendedProjectsRes getTopRecommendedProjects(Long memberId) {
        // TODO: 실제 추천 알고리즘 구현
        // 현재는 최신 생성된 프로젝트 4개 반환
        List<Project> projects = projectRepository.findAllActiveProjects(4);

        List<ProjectResDTO.RecommendedProjectSummary> topProjects = projects.stream()
                .map(project -> ProjectConverter.toRecommendedProjectSummary(project, projectRequirementTechstackRepository))
                .toList();

        return ProjectResDTO.TopRecommendedProjectsRes.builder()
                .projects(topProjects)
                .build();
    }

    @Override
    public ProjectResDTO.SearchProjectsRes searchProjects(ProjectReqDTO.SearchProjectReq request) {
        int pageIndex = request.page() - 1;
        Pageable pageable = PageRequest.of(pageIndex, request.size());

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
    public ProjectResDTO.SearchRecommendedProjectsRes searchRecommendedProjects(
            Long memberId,
            ProjectReqDTO.SearchRecommendedProjectReq request
    ) {
        int pageIndex = request.page() - 1;
        Pageable pageable = PageRequest.of(pageIndex, request.size());
        Predicate predicate = ProjectPredicateBuilder.buildRecommendedPredicate(request);

        // TODO: 추천 알고리즘 기반 정렬 추가
        Page<Project> projectPage = projectRepository.searchRecommendedProjects(predicate, pageable);

        List<ProjectResDTO.RecommendedProjectSummary> summaries = projectPage.getContent().stream()
                .map(project -> ProjectConverter.toRecommendedProjectSummary(project, projectRequirementTechstackRepository))
                .toList();

        PagedResponse<ProjectResDTO.RecommendedProjectSummary> pagedData = PagedResponse.of(projectPage, summaries);

        return ProjectResDTO.SearchRecommendedProjectsRes.builder()
                .projects(pagedData)
                .build();
    }
}