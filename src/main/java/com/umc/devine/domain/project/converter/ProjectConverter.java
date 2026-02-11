package com.umc.devine.domain.project.converter;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectImage;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.ProjectPart;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectConverter {

    // CreateProjectReq → Project 엔티티 변환
    public static Project toProject(ProjectReqDTO.CreateProjectReq request, Member member, Category category) {
        return Project.builder()
                .member(member)
                .category(category)
                .projectField(request.projectField())
                .mode(request.mode())
                .durationRange(request.durationRange())
                .location(request.location())
                .recruitmentDeadline(request.recruitmentDeadline())
                .name(request.title())
                .content(request.content())
                .status(ProjectStatus.RECRUITING)
                .images(new ArrayList<>())
                .requirements(new ArrayList<>())
                .build();
    }

    // Image 엔티티 → ProjectImage 엔티티 변환
    public static ProjectImage toProjectImageFromImage(Image image, Project project) {
        return ProjectImage.builder()
                .project(project)
                .image(image)
                .build();
    }

    // RecruitmentDTO → ProjectRequirementMember 엔티티 변환
    public static ProjectRequirementMember toProjectRequirementMember(
            ProjectReqDTO.RecruitmentDTO recruitment,
            Project project
    ) {
        return ProjectRequirementMember.builder()
                .project(project)
                .part(recruitment.position())
                .requirementNum(recruitment.count())
                .currentCount(0)
                .build();
    }

    // Project → CreateProjectRes 변환 (프로젝트 생성 응답)
    public static ProjectResDTO.CreateProjectRes toCreateProjectRes(
            Project project,
            ProjectRequirementTechstackRepository projectRequirementTechstackRepository
    ) {
        return ProjectResDTO.CreateProjectRes.builder()
                .projectId(project.getId())
                .projectField(project.getProjectField())
                .projectFieldName(project.getProjectField().getDisplayName())
                .category(project.getCategory().getGenre())
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .durationRange(project.getDurationRange())
                .durationRangeName(project.getDurationRange().getDisplayName())
                .location(project.getLocation())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
                .title(project.getTitle())
                .content(project.getContent())
                .status(project.getStatus())
                .creatorId(project.getMember().getId())
                .creatorNickname(project.getMember().getNickname())
                .recruitments(toRecruitmentInfoList(project.getRequirements(), projectRequirementTechstackRepository))
                .images(toImageInfoList(project.getImages()))
                .build();
    }

    // Project → UpdateProjectRes 변환 (프로젝트 수정/조회 응답)
    public static ProjectResDTO.UpdateProjectRes toUpdateProjectRes(
            Project project,
            ProjectRequirementTechstackRepository projectRequirementTechstackRepository
    ) {
        return ProjectResDTO.UpdateProjectRes.builder()
                .projectId(project.getId())
                .projectField(project.getProjectField())
                .projectFieldName(project.getProjectField().getDisplayName())
                .category(project.getCategory().getGenre())
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .durationRange(project.getDurationRange())
                .durationRangeName(project.getDurationRange().getDisplayName())
                .location(project.getLocation())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
                .title(project.getTitle())
                .content(project.getContent())
                .status(project.getStatus())
                .creatorId(project.getMember().getId())
                .creatorNickname(project.getMember().getNickname())
                .creatorImage(project.getMember().getImage())
                .recruitments(toRecruitmentInfoList(project.getRequirements(), projectRequirementTechstackRepository))
                .images(toImageInfoList(project.getImages()))
                .build();
    }

    // Project → ProjectSummary 변환 (검색 결과 요약 - 포지션별 모집 정보 포함)
    public static ProjectResDTO.ProjectSummary toProjectSummary(
            Project project,
            ProjectRequirementTechstackRepository techstackRepository
    ) {
        List<ProjectResDTO.PositionSummary> positions = project.getRequirements().stream()
                .map(req -> {
                    List<ProjectResDTO.TechStackInfo> techStacks = techstackRepository.findByRequirement(req).stream()
                            .map(reqTechstack -> ProjectResDTO.TechStackInfo.builder()
                                    .techStack(reqTechstack.getTechstack().getName())
                                    .build())
                            .toList();

                    return ProjectResDTO.PositionSummary.builder()
                            .position(req.getPart())
                            .positionName(req.getPart().getDisplayName())
                            .count(req.getRequirementNum())
                            .currentCount(req.getCurrentCount())
                            .techStacks(techStacks)
                            .build();
                })
                .toList();

        return ProjectResDTO.ProjectSummary.builder()
                .projectId(project.getId())
                .title(project.getTitle())
                .projectField(project.getProjectField())
                .projectFieldName(project.getProjectField().getDisplayName())
                .category(project.getCategory().getGenre())
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .durationRange(project.getDurationRange())
                .durationRangeName(project.getDurationRange().getDisplayName())
                .location(project.getLocation())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
                .status(project.getStatus())
                .thumbnailUrl(project.getImages().isEmpty() ? null : project.getImages().get(0).getImageUrl())
                .positions(positions)
                .creatorNickname(project.getMember().getNickname())
                .build();
    }

    // Project → RecommendedProjectSummary 변환 (추천 프로젝트 요약 - 벡터 검색 점수 포함)
    public static ProjectResDTO.RecommendedProjectSummary toRecommendedProjectSummary(
            Project project,
            ProjectRequirementTechstackRepository techstackRepository,
            Double totalScore,
            Double similarityScorePercent,
            Double techstackScorePercent,
            Boolean domainMatch
    ) {
        List<ProjectResDTO.PositionSummary> positions = project.getRequirements().stream()
                .map(req -> {
                    List<ProjectResDTO.TechStackInfo> techStacks = techstackRepository.findByRequirement(req).stream()
                            .map(reqTechstack -> ProjectResDTO.TechStackInfo.builder()
                                    .techStack(reqTechstack.getTechstack().getName())
                                    .build())
                            .toList();

                    return ProjectResDTO.PositionSummary.builder()
                            .position(req.getPart())
                            .positionName(req.getPart().getDisplayName())
                            .count(req.getRequirementNum())
                            .currentCount(req.getCurrentCount())
                            .techStacks(techStacks)
                            .build();
                })
                .toList();

        return ProjectResDTO.RecommendedProjectSummary.builder()
                .projectId(project.getId())
                .title(project.getTitle())
                .projectField(project.getProjectField())
                .projectFieldName(project.getProjectField().getDisplayName())
                .category(project.getCategory().getGenre())
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .durationRange(project.getDurationRange())
                .durationRangeName(project.getDurationRange().getDisplayName())
                .location(project.getLocation())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
                .status(project.getStatus())
                .thumbnailUrl(project.getImages().isEmpty() ? null : project.getImages().get(0).getImageUrl())
                .positions(positions)
                .creatorNickname(project.getMember().getNickname())
                .totalScore(totalScore != null ? Math.round(totalScore * 10) / 10.0 : null)
                .similarityScorePercent(similarityScorePercent != null ? Math.round(similarityScorePercent * 10) / 10.0 : null)
                .techstackScorePercent(techstackScorePercent != null ? Math.round(techstackScorePercent * 10) / 10.0 : null)
                .domainMatch(domainMatch)
                .build();
    }

    // ProjectRequirementMember 리스트 → RecruitmentInfo 리스트 변환 (기술 스택 포함)
    private static List<ProjectResDTO.RecruitmentInfo> toRecruitmentInfoList(
            List<ProjectRequirementMember> requirements,
            ProjectRequirementTechstackRepository techstackRepository
    ) {
        return requirements.stream()
                .map(req -> {
                    List<Techstack> techstacks = techstackRepository.findByRequirement(req).stream()
                            .map(ProjectRequirementTechstack::getTechstack)
                            .toList();

                    return ProjectResDTO.RecruitmentInfo.builder()
                            .position(req.getPart())
                            .positionName(req.getPart().getDisplayName())
                            .count(req.getRequirementNum())
                            .currentCount(req.getCurrentCount())
                            .techStacks(
                                    techstacks.stream()
                                            .map(ts -> ProjectResDTO.TechStackInfo.builder()
                                                    .techStack(ts.getName())
                                                    .build())
                                            .toList()
                            )
                            .build();
                })
                .toList();
    }

    // ProjectImage 리스트 → ImageInfo 리스트 변환
    private static List<ProjectResDTO.ImageInfo> toImageInfoList(List<ProjectImage> images) {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }
        return images.stream()
                .map(img -> ProjectResDTO.ImageInfo.builder()
                        .imageId(img.getImage().getId())
                        .imageUrl(img.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }

    // 모집 마감까지 남은 일수 계산
    private static Long calculateDaysUntilDeadline(LocalDate recruitmentDeadline) {
        LocalDate today = LocalDate.now();
        return ChronoUnit.DAYS.between(today, recruitmentDeadline);
    }

    // PM용: Project → MyProjectInfo (myPart = PM)
    public static ProjectResDTO.MyProjectInfo toMyProjectInfo(Project project) {
        return buildMyProjectInfo(project, ProjectPart.PM);
    }

    // 개발자용: Matching → MyProjectInfo (myPart = matching.getPart())
    public static ProjectResDTO.MyProjectInfo toMyProjectInfo(Matching matching) {
        return buildMyProjectInfo(matching.getProject(), matching.getPart());
    }

    private static ProjectResDTO.MyProjectInfo buildMyProjectInfo(Project project, ProjectPart myPart) {
        String thumbnailUrl = project.getImages().isEmpty()
                ? null
                : project.getImages().get(0).getImageUrl();

        return ProjectResDTO.MyProjectInfo.builder()
                .projectId(project.getId())
                .title(project.getTitle())
                .thumbnailUrl(thumbnailUrl)
                .projectField(project.getProjectField())
                .projectFieldName(project.getProjectField().getDisplayName())
                .category(project.getCategory().getGenre())
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .location(project.getLocation())
                .durationRange(project.getDurationRange())
                .durationRangeName(project.getDurationRange().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .myPart(myPart)
                .myPartName(myPart != null ? myPart.getDisplayName() : null)
                .projectStatus(project.getStatus())
                .projectStatusName(project.getStatus().getDisplayName())
                .build();
    }
}