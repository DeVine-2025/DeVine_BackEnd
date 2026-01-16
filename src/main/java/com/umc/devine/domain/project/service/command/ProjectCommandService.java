package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.exception.CategoryException;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.enums.MemberMainType;
import com.umc.devine.domain.member.exception.MemberException;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.domain.project.converter.ProjectConverter;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectImage;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.repository.ProjectImageRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.project.validator.ProjectValidator;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.exception.TechstackException;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.umc.devine.domain.category.exception.code.CategoryErrorCode.CATEGORY_NOT_FOUND;
import static com.umc.devine.domain.member.exception.code.MemberErrorCode.MEMBER_NOT_FOUND;
import static com.umc.devine.domain.project.exception.code.ProjectErrorCode.INVALID_PERMISSION;
import static com.umc.devine.domain.project.exception.code.ProjectErrorCode.PROJECT_NOT_FOUND;
import static com.umc.devine.domain.techstack.exception.code.TechstackErrorCode.TECHSTACK_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandService {

    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final ProjectRequirementMemberRepository projectRequirementMemberRepository;
    private final ProjectRequirementTechstackRepository projectRequirementTechstackRepository;
    private final TechstackRepository techstackRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ProjectValidator projectValidator;

    public ProjectResDTO.CreateProjectRes createProject(Long memberId, ProjectReqDTO.CreateProjectReq request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MEMBER_NOT_FOUND));

        if (member.getMainType() != MemberMainType.PM) {
            throw new ProjectException(INVALID_PERMISSION);
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryException(CATEGORY_NOT_FOUND));

        projectValidator.validateRecruitmentDeadline(request.recruitmentDeadline());

        Project project = ProjectConverter.toProject(request, member, category);
        Project savedProject = projectRepository.save(project);

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            List<ProjectImage> images = request.imageUrls().stream()
                    .map(url -> ProjectConverter.toProjectImage(url, savedProject))
                    .collect(Collectors.toList());
            projectImageRepository.saveAll(images).forEach(savedProject::addImage);
        }

        if (request.recruitments() != null && !request.recruitments().isEmpty()) {
            List<ProjectRequirementMember> requirements = request.recruitments().stream()
                    .map(recruitment -> ProjectConverter.toProjectRequirementMember(recruitment, savedProject))
                    .collect(Collectors.toList());
            projectRequirementMemberRepository.saveAll(requirements).forEach(savedProject::addRequirement);

            saveTechstacks(savedProject, request.recruitments());
        }

        return ProjectConverter.toCreateProjectRes(savedProject, projectRequirementTechstackRepository);
    }

    public ProjectResDTO.UpdateProjectRes updateProject(Long memberId, Long projectId, ProjectReqDTO.UpdateProjectReq request) {
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        projectValidator.validateOwner(project, memberId);

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryException(CATEGORY_NOT_FOUND));

        projectValidator.validateRecruitmentDeadline(request.recruitmentDeadline());

        project.updateProjectInfo(
                request.projectField(),
                category,
                request.mode(),
                request.durationMonths(),
                request.location(),
                request.recruitmentDeadline(),
                request.startDate()
        );

        project.updateContent(
                request.title(),
                request.content()
        );

        projectImageRepository.deleteAllByProject(project);
        project.clearImages();

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            List<ProjectImage> newImages = request.imageUrls().stream()
                    .map(url -> ProjectConverter.toProjectImage(url, project))
                    .collect(Collectors.toList());
            projectImageRepository.saveAll(newImages).forEach(project::addImage);
        }

        List<ProjectRequirementMember> oldRequirements =
                projectRequirementMemberRepository.findAllByProject(project);

        if (!oldRequirements.isEmpty()) {
            projectRequirementTechstackRepository.deleteAllByRequirementIn(oldRequirements);
        }

        projectRequirementMemberRepository.deleteAllByProject(project);
        project.clearRequirements();

        projectRequirementMemberRepository.flush();

        if (request.recruitments() != null && !request.recruitments().isEmpty()) {
            List<ProjectRequirementMember> newRequirements = request.recruitments().stream()
                    .map(recruitment -> ProjectConverter.toProjectRequirementMember(recruitment, project))
                    .collect(Collectors.toList());

            List<ProjectRequirementMember> savedRequirements =
                    projectRequirementMemberRepository.saveAll(newRequirements);
            projectRequirementMemberRepository.flush();

            savedRequirements.forEach(project::addRequirement);

            saveTechstacks(project, request.recruitments());
        }

        return ProjectConverter.toUpdateProjectRes(project, projectRequirementTechstackRepository);
    }

    public void deleteProject(Long memberId, Long projectId) {
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        projectValidator.validateOwner(project, memberId);

        project.delete();
    }

    private void saveTechstacks(Project project, List<ProjectReqDTO.RecruitmentDTO> recruitments) {
        List<ProjectRequirementMember> requirementEntities = project.getRequirements();
        List<ProjectRequirementTechstack> all = new ArrayList<>();

        for (int i = 0; i < recruitments.size(); i++) {
            ProjectReqDTO.RecruitmentDTO dto = recruitments.get(i);
            ProjectRequirementMember requirement = requirementEntities.get(i);

            if (dto.techStackIds() == null || dto.techStackIds().isEmpty()) {
                continue;
            }

            for (Long techstackId : dto.techStackIds()) {
                Techstack techstack = techstackRepository.findById(techstackId)
                        .orElseThrow(() -> new TechstackException(TECHSTACK_NOT_FOUND));

                ProjectRequirementTechstack mapping = ProjectRequirementTechstack.builder()
                        .requirement(requirement)
                        .techstack(techstack)
                        .build();

                all.add(mapping);
            }
        }

        if (!all.isEmpty()) {
            projectRequirementTechstackRepository.saveAll(all);
        }
    }
}