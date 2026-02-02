package com.umc.devine.domain.project.converter;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectImage;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
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
                .durationMonths(request.durationMonths())
                .location(request.location())
                .recruitmentDeadline(request.recruitmentDeadline())
                .name(request.title())
                .content(request.content())
                .status(ProjectStatus.RECRUITING)
                .images(new ArrayList<>())
                .requirements(new ArrayList<>())
                .build();
    }

    // 이미지 URL → ProjectImage 엔티티 변환
    public static ProjectImage toProjectImage(String imageUrl, Project project) {
        return ProjectImage.builder()
                .project(project)
                .image(imageUrl)
                .build();
    }

    // Image 엔티티 → ProjectImage 엔티티 변환
    public static ProjectImage toProjectImageFromImage(Image image, Project project) {
        return ProjectImage.builder()
                .project(project)
                .image(image.getImageUrl())
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
                .categoryId(project.getCategory().getId())
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .durationMonths(project.getDurationMonths())
                .location(project.getLocation())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
                .title(project.getTitle())
                .content(project.getContent())
                .status(project.getStatus())
                .creatorId(project.getMember().getId())
                .creatorName(project.getMember().getName())
                .recruitments(toRecruitmentInfoList(project.getRequirements(), projectRequirementTechstackRepository))
                .imageUrls(toImageUrlList(project.getImages()))
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
                .categoryId(project.getCategory().getId())
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .durationMonths(project.getDurationMonths())
                .location(project.getLocation())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
                .title(project.getTitle())
                .content(project.getContent())
                .status(project.getStatus())
                .creatorId(project.getMember().getId())
                .creatorName(project.getMember().getName())
                .recruitments(toRecruitmentInfoList(project.getRequirements(), projectRequirementTechstackRepository))
                .imageUrls(toImageUrlList(project.getImages()))
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
                                    .techStackId(reqTechstack.getTechstack().getId())
                                    .techStackName(reqTechstack.getTechstack().getName().name())
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
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .durationMonths(project.getDurationMonths())
                .location(project.getLocation())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
                .status(project.getStatus())
                .thumbnailUrl(project.getImages().isEmpty() ? null : project.getImages().get(0).getImage())
                .positions(positions)
                .creatorName(project.getMember().getName())
                .build();
    }

    // Project → RecommendedProjectSummary 변환 (추천 프로젝트 요약 - 점수 포함, 포지션별 모집 정보 포함)
    public static ProjectResDTO.RecommendedProjectSummary toRecommendedProjectSummary(
            Project project,
            ProjectRequirementTechstackRepository techstackRepository
    ) {
        // TODO: 실제 추천 알고리즘 기반 점수 계산
        // 현재는 더미 점수 반환
        int techScore = 4;
        int domainScore = 4;
        int techStackCountScore = 3;
        int totalScore = calculateTotalScore(techScore, domainScore, techStackCountScore);

        List<ProjectResDTO.PositionSummary> positions = project.getRequirements().stream()
                .map(req -> {
                    List<ProjectResDTO.TechStackInfo> techStacks = techstackRepository.findByRequirement(req).stream()
                            .map(reqTechstack -> ProjectResDTO.TechStackInfo.builder()
                                    .techStackId(reqTechstack.getTechstack().getId())
                                    .techStackName(reqTechstack.getTechstack().getName().name())
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
                .categoryName(project.getCategory().getGenre().getDisplayName())
                .mode(project.getMode())
                .modeName(project.getMode().getDisplayName())
                .durationMonths(project.getDurationMonths())
                .location(project.getLocation())
                .recruitmentDeadline(project.getRecruitmentDeadline())
                .daysUntilDeadline(calculateDaysUntilDeadline(project.getRecruitmentDeadline()))
                .status(project.getStatus())
                .thumbnailUrl(project.getImages().isEmpty() ? null : project.getImages().get(0).getImage())
                .positions(positions)
                .creatorName(project.getMember().getName())
                .techScore(techScore)
                .domainScore(domainScore)
                .techStackCountScore(techStackCountScore)
                .totalScore(totalScore)
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
                            .count(req.getRequirementNum())
                            .currentCount(req.getCurrentCount())
                            .techStacks(
                                    techstacks.stream()
                                            .map(ts -> ProjectResDTO.TechStackInfo.builder()
                                                    .techStackId(ts.getId())
                                                    .techStackName(ts.getName().name())
                                                    .build())
                                            .toList()
                            )
                            .build();
                })
                .toList();
    }

    // ProjectImage 리스트 → 이미지 URL 리스트 변환
    private static List<String> toImageUrlList(List<ProjectImage> images) {
        if (images == null || images.isEmpty()) {
            return new ArrayList<>();
        }
        return images.stream()
                .map(ProjectImage::getImage)
                .collect(Collectors.toList());
    }

    // 모집 마감까지 남은 일수 계산
    private static Long calculateDaysUntilDeadline(LocalDate recruitmentDeadline) {
        LocalDate today = LocalDate.now();
        return ChronoUnit.DAYS.between(today, recruitmentDeadline);
    }

    // 총점 계산 (5점 만점 점수들을 100점 만점으로 환산)
    private static int calculateTotalScore(int techScore, int domainScore, int techStackCountScore) {
        double techWeight = (techScore / 5.0) * 100 / 3.0;
        double domainWeight = (domainScore / 5.0) * 100 / 3.0;
        double techStackWeight = (techStackCountScore / 5.0) * 100 / 3.0;
        return (int) Math.round(techWeight + domainWeight + techStackWeight);
    }

    public static ProjectResDTO.ProjectDetailDTO toProjectDetail(Project project, List<ProjectImage> images) {
        List<String> imageUrls = (images != null) ? images.stream()
                .map(ProjectImage::getImage)
                .collect(Collectors.toList()) : List.of();
        return ProjectResDTO.ProjectDetailDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .content(project.getContent())
                .status(project.getStatus())
                .imageUrls(imageUrls)
                .build();
    }

    public static ProjectResDTO.ProjectListDTO toProjectList(List<ProjectResDTO.ProjectDetailDTO> projectInfoList) {
        return ProjectResDTO.ProjectListDTO.builder()
                .projects(projectInfoList)
                .build();
    }
}