package com.umc.devine.domain.project.service.query;

import com.querydsl.core.types.Predicate;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.converter.ProjectConverter;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.repository.MatchingRepository;
import com.umc.devine.domain.project.repository.ProjectRecommendRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.querydsl.ProjectPredicateBuilder;
import com.umc.devine.domain.report.entity.ReportEmbedding;
import com.umc.devine.domain.report.repository.ReportEmbeddingRepository;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import com.umc.devine.global.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.umc.devine.domain.project.exception.code.ProjectErrorCode.PROJECT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryServiceImpl implements ProjectQueryService {

    private static final int WEEKLY_BEST_LIMIT = 4;
    private static final int RECOMMEND_LIMIT = 10;

    private final ProjectRepository projectRepository;
    private final MatchingRepository matchingRepository;
    private final ProjectRequirementTechstackRepository projectRequirementTechstackRepository;
    private final ProjectRecommendRepository projectRecommendRepository;
    private final ReportEmbeddingRepository reportEmbeddingRepository;

    @Override
    @Transactional
    public ProjectResDTO.UpdateProjectRes getProjectDetail(Long projectId) {
        Project project = projectRepository.findByIdWithMemberAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        // 원자적 조회수 증가 (동시성 안전)
        projectRepository.incrementViewCount(projectId);

        Map<Long, List<ProjectRequirementTechstack>> techstackMap = buildTechstackMap(List.of(projectId));
        return ProjectConverter.toUpdateProjectRes(project, techstackMap);
    }

    @Override
    public ProjectResDTO.WeeklyBestProjectsRes getWeeklyBestProjects() {
        boolean isMonday = LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY;
        List<Project> projects = projectRepository.findWeeklyBestProjects(ProjectStatus.DELETED, isMonday);

        List<Project> limitedProjects = projects.stream().limit(WEEKLY_BEST_LIMIT).toList();
        List<Long> projectIds = limitedProjects.stream().map(Project::getId).toList();
        Map<Long, List<ProjectRequirementTechstack>> techstackMap = buildTechstackMap(projectIds);

        List<ProjectResDTO.ProjectSummary> weeklyBestProjects = limitedProjects.stream()
                .map(project -> ProjectConverter.toProjectSummary(project, techstackMap))
                .toList();

        return ProjectResDTO.WeeklyBestProjectsRes.builder()
                .projects(weeklyBestProjects)
                .build();
    }

    @Override
    public ProjectResDTO.SearchProjectsRes searchProjects(ProjectReqDTO.SearchProjectReq request) {
        Pageable pageable = request.toPageable();

        Predicate predicate = ProjectPredicateBuilder.buildSearchPredicate(
                request.projectFields(),
                request.categories(),
                request.durationRanges(),
                request.positions(),
                request.techstackNames()
        );
        Page<Project> projectPage = projectRepository.searchProjects(predicate, pageable);

        List<Long> projectIds = projectPage.getContent().stream().map(Project::getId).toList();
        Map<Long, List<ProjectRequirementTechstack>> techstackMap = buildTechstackMap(projectIds);

        List<ProjectResDTO.ProjectSummary> summaries = projectPage.getContent().stream()
                .map(project -> ProjectConverter.toProjectSummary(project, techstackMap))
                .toList();

        PagedResponse<ProjectResDTO.ProjectSummary> pagedData = PagedResponse.of(projectPage, summaries);

        return ProjectResDTO.SearchProjectsRes.builder()
                .projects(pagedData)
                .build();
    }

    @Override
    public ProjectResDTO.RecommendedProjectsRes getRecommendedProjectsPreview(
            Member member,
            ProjectReqDTO.RecommendProjectsPreviewReq request
    ) {
        int limit = request.limit();

        // 개발자이고 리포트 임베딩이 있으면 벡터 검색
        Optional<ReportEmbedding> embedding = reportEmbeddingRepository.findLatestByMemberId(member.getId());
        if (embedding.isPresent()) {
            List<ProjectResDTO.RecommendedProjectSummary> summaries =
                    executeVectorSearch(member.getId(), embedding.get().getId(), limit, null, null, null, null);

            if (!summaries.isEmpty()) {
                return ProjectResDTO.RecommendedProjectsRes.builder()
                        .projects(summaries)
                        .count(summaries.size())
                        .build();
            }
        }

        return ProjectResDTO.RecommendedProjectsRes.builder()
                .projects(List.of())
                .count(0)
                .build();
    }

    @Override
    public ProjectResDTO.RecommendedProjectsRes getRecommendedProjects(
            Member member,
            ProjectReqDTO.RecommendProjectsReq request
    ) {
        // 개발자이고 리포트 임베딩이 있으면 벡터 검색
        Optional<ReportEmbedding> embedding = reportEmbeddingRepository.findLatestByMemberId(member.getId());

        if (embedding.isEmpty()) {
            return ProjectResDTO.RecommendedProjectsRes.builder()
                    .projects(List.of())
                    .count(0)
                    .build();
        }

        List<ProjectResDTO.RecommendedProjectSummary> summaries =
                executeVectorSearch(
                        member.getId(),
                        embedding.get().getId(),
                        RECOMMEND_LIMIT,
                        request.projectFields(),
                        request.categories(),
                        request.techstackNames(),
                        request.durationRanges()
                );

        return ProjectResDTO.RecommendedProjectsRes.builder()
                .projects(summaries)
                .count(summaries.size())
                .build();
    }

    @Override
    public ProjectResDTO.MyProjectsRes getMyProjects(Member member, List<ProjectStatus> statuses, Pageable pageable) {
        // 내가 등록한 프로젝트
        List<ProjectResDTO.MyProjectInfo> createdInfos = projectRepository
                .findAllByMemberAndStatusIn(member, statuses)
                .stream()
                .map(ProjectConverter::toMyProjectInfo)
                .toList();

        // 매칭 수락된 프로젝트
        List<ProjectResDTO.MyProjectInfo> matchedInfos = matchingRepository
                .findAllByMemberAndDecisionAndProjectStatusIn(member, MatchingDecision.ACCEPT, statuses)
                .stream()
                .map(ProjectConverter::toMyProjectInfo)
                .toList();

        // 중복 제거: 내가 등록한 프로젝트 우선, 매칭 프로젝트 중 중복 제외
        Set<Long> createdProjectIds = createdInfos.stream()
                .map(ProjectResDTO.MyProjectInfo::projectId)
                .collect(Collectors.toSet());

        List<ProjectResDTO.MyProjectInfo> combined = new ArrayList<>(createdInfos);
        matchedInfos.stream()
                .filter(info -> !createdProjectIds.contains(info.projectId()))
                .forEach(combined::add);

        // 수동 페이지네이션
        int total = combined.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<ProjectResDTO.MyProjectInfo> pageContent = start >= total ? List.of() : combined.subList(start, end);

        int pageSize = pageable.getPageSize();
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);

        PagedResponse<ProjectResDTO.MyProjectInfo> pagedResponse = PagedResponse.<ProjectResDTO.MyProjectInfo>builder()
                .content(pageContent)
                .page(pageable.getPageNumber() + 1)
                .size(pageSize)
                .totalElements(total)
                .totalPages(totalPages)
                .isFirst(pageable.getPageNumber() == 0)
                .isLast(end >= total)
                .build();

        return ProjectResDTO.MyProjectsRes.builder()
                .projects(pagedResponse)
                .build();
    }

    @Override
    public ProjectResDTO.MyProjectsRes getMyCreatedRecruitingProjects(Member member, Pageable pageable) {
        List<ProjectResDTO.MyProjectInfo> createdInfos = projectRepository
                .findAllByMemberAndStatusIn(member, List.of(ProjectStatus.RECRUITING))
                .stream()
                .map(ProjectConverter::toMyProjectInfo)
                .toList();

        int total = createdInfos.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<ProjectResDTO.MyProjectInfo> pageContent = start >= total ? List.of() : createdInfos.subList(start, end);

        int pageSize = pageable.getPageSize();
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);

        PagedResponse<ProjectResDTO.MyProjectInfo> pagedResponse = PagedResponse.<ProjectResDTO.MyProjectInfo>builder()
                .content(pageContent)
                .page(pageable.getPageNumber() + 1)
                .size(pageSize)
                .totalElements(total)
                .totalPages(totalPages)
                .isFirst(pageable.getPageNumber() == 0)
                .isLast(end >= total)
                .build();

        return ProjectResDTO.MyProjectsRes.builder()
                .projects(pagedResponse)
                .build();
    }

    // ==================== Private Methods ====================

    /**
     * 프로젝트 ID 목록으로 기술스택을 배치 조회하여 requirementId 기준 Map으로 반환.
     * N+1 방지: 모든 프로젝트의 기술스택을 단 1번의 쿼리로 조회.
     */
    private Map<Long, List<ProjectRequirementTechstack>> buildTechstackMap(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return projectRequirementTechstackRepository.findAllByProjectIdsWithTechstack(projectIds)
                .stream()
                .collect(Collectors.groupingBy(prt -> prt.getRequirement().getId()));
    }

    /**
     * 벡터 검색 기반 프로젝트 추천 실행.
     *
     * Object[] 구조:
     * [0] project_id, [1] similarity_score, [2] techstack_score, [3] domain_score,
     * [4] total_score, [5] similarity_score_percent, [6] techstack_score_percent, [7] domain_match
     */
    private List<ProjectResDTO.RecommendedProjectSummary> executeVectorSearch(
            Long memberId,
            Long embeddingId,
            int limit,
            List<ProjectField> projectFields,
            List<CategoryGenre> categories,
            List<TechName> techstackNames,
            List<DurationRange> durationRanges
    ) {
        List<Object[]> results = projectRecommendRepository.findRecommendedProjects(
                memberId,
                embeddingId,
                limit,
                projectFields,
                categories,
                techstackNames,
                durationRanges
        );

        if (results.isEmpty()) {
            return List.of();
        }

        // N+1 방지: projectIds 추출 후 IN 쿼리로 한 번에 조회
        List<Long> projectIds = results.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        List<Project> projects = projectRepository.findAllByIdIn(projectIds);
        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        // 기술스택도 배치 조회
        Map<Long, List<ProjectRequirementTechstack>> techstackMap = buildTechstackMap(projectIds);

        return results.stream()
                .map(row -> {
                    Long projectId = ((Number) row[0]).longValue();
                    Double totalScore = ((Number) row[4]).doubleValue();
                    Double similarityScorePercent = ((Number) row[5]).doubleValue();
                    Double techstackScorePercent = ((Number) row[6]).doubleValue();
                    Boolean domainMatch = row[7] == null ? null : Boolean.parseBoolean(row[7].toString());

                    Project project = projectMap.get(projectId);
                    if (project == null) {
                        return null;
                    }

                    return ProjectConverter.toRecommendedProjectSummary(
                            project,
                            techstackMap,
                            totalScore,
                            similarityScorePercent,
                            techstackScorePercent,
                            domainMatch
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // 기본 추천: 임베딩이 없는 경우 최신 모집 중 프로젝트 반환
    private ProjectResDTO.RecommendedProjectsRes getDefaultRecommendations(int limit) {
        List<Project> projects = projectRepository.findByStatusOrderByCreatedAtDesc(ProjectStatus.RECRUITING)
                .stream()
                .limit(limit)
                .toList();

        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        Map<Long, List<ProjectRequirementTechstack>> techstackMap = buildTechstackMap(projectIds);

        List<ProjectResDTO.RecommendedProjectSummary> summaries = projects.stream()
                .map(project -> ProjectConverter.toRecommendedProjectSummary(
                        project, techstackMap,
                        null, null, null, null))
                .toList();

        return ProjectResDTO.RecommendedProjectsRes.builder()
                .projects(summaries)
                .count(summaries.size())
                .build();
    }

}
