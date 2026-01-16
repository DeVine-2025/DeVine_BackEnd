package com.umc.devine.domain.project.converter;

import com.umc.devine.domain.category.entity.Category;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectConverter {

    // CreateProjectReq → Project 엔티티 변환
    public static Project toProject(ProjectReqDTO.CreateProjectReq request, Member member, Category category) {
        LocalDate eta = request.startDate().plusMonths(request.durationMonths());

        return Project.builder()
                .member(member)
                .category(category)
                .projectField(request.projectField())
                .mode(request.mode())
                .durationMonths(request.durationMonths())
                .location(request.location())
                .recruitmentDeadline(request.recruitmentDeadline())
                .startDate(request.startDate())
                .eta(eta)
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
                .startDate(project.getStartDate())
                .eta(project.getEta())
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
                .startDate(project.getStartDate())
                .eta(project.getEta())
                .title(project.getTitle())
                .content(project.getContent())
                .status(project.getStatus())
                .creatorId(project.getMember().getId())
                .creatorName(project.getMember().getName())
                .recruitments(toRecruitmentInfoList(project.getRequirements(), projectRequirementTechstackRepository))
                .imageUrls(toImageUrlList(project.getImages()))
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
}
