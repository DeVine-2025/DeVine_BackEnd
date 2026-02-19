package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.exception.CategoryException;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.exception.code.ImageErrorCode;
import com.umc.devine.domain.image.repository.ImageRepository;
import com.umc.devine.domain.member.entity.Member;
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
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.exception.TechstackException;
import com.umc.devine.domain.techstack.exception.code.TechstackErrorCode;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import com.umc.devine.domain.techstack.repository.TechstackRepository;
import com.umc.devine.domain.project.event.ProjectEmbeddingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.time.LocalDate;

import static com.umc.devine.domain.category.exception.code.CategoryErrorCode.CATEGORY_NOT_FOUND;
import static com.umc.devine.domain.project.exception.code.ProjectErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectCommandServiceImpl implements ProjectCommandService {

    private final ProjectRepository projectRepository;
    private final ProjectImageRepository projectImageRepository;
    private final ProjectRequirementMemberRepository projectRequirementMemberRepository;
    private final ProjectRequirementTechstackRepository projectRequirementTechstackRepository;
    private final TechstackRepository techstackRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ProjectResDTO.CreateProjectRes createProject(Member member, ProjectReqDTO.CreateProjectReq request) {
        Category category = categoryRepository.findByGenre(request.category())
                .orElseThrow(() -> new CategoryException(CATEGORY_NOT_FOUND));

        validateRecruitmentDeadline(request.recruitmentDeadline());

        Project project = ProjectConverter.toProject(request, member, category);
        Project savedProject = projectRepository.save(project);

        saveProjectImages(request.imageIds(), savedProject, member.getId());

        if (request.recruitments() != null && !request.recruitments().isEmpty()) {
            List<ProjectRequirementMember> requirements = request.recruitments().stream()
                    .map(recruitment -> ProjectConverter.toProjectRequirementMember(recruitment, savedProject))
                    .collect(Collectors.toList());
            projectRequirementMemberRepository.saveAll(requirements).forEach(savedProject::addRequirement);

            saveTechstacks(savedProject, request.recruitments());
        }

        // 프로젝트 임베딩 요청 이벤트 발행 (트랜잭션 커밋 후 비동기 처리)
        eventPublisher.publishEvent(ProjectEmbeddingEvent.builder()
                .projectId(savedProject.getId())
                .content(savedProject.getContent())
                .build());

        return ProjectConverter.toCreateProjectRes(savedProject, projectRequirementTechstackRepository);
    }

    @Override
    public ProjectResDTO.UpdateProjectRes updateProject(Member member, Long projectId, ProjectReqDTO.UpdateProjectReq request) {
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        validateOwner(project, member.getId());

        Category category = categoryRepository.findByGenre(request.category())
                .orElseThrow(() -> new CategoryException(CATEGORY_NOT_FOUND));

        validateRecruitmentDeadline(request.recruitmentDeadline());

        project.updateProjectInfo(
                request.projectField(),
                category,
                request.mode(),
                request.durationRange(),
                request.location(),
                request.recruitmentDeadline()
        );

        project.updateContent(
                request.title(),
                request.content()
        );

        projectImageRepository.deleteAllByProject(project);
        project.clearImages();

        saveProjectImages(request.imageIds(), project, member.getId());

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

        // 프로젝트 임베딩 업데이트 요청 이벤트 발행 (트랜잭션 커밋 후 비동기 처리)
        eventPublisher.publishEvent(ProjectEmbeddingEvent.builder()
                .projectId(project.getId())
                .content(project.getContent())
                .build());

        return ProjectConverter.toUpdateProjectRes(project, projectRequirementTechstackRepository);
    }

    @Override
    public void deleteProject(Member member, Long projectId) {
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        validateOwner(project, member.getId());

        project.delete();
    }

    private void saveTechstacks(Project project, List<ProjectReqDTO.RecruitmentDTO> recruitments) {
        List<ProjectRequirementMember> requirementEntities = project.getRequirements();
        List<ProjectRequirementTechstack> all = new ArrayList<>();

        for (int i = 0; i < recruitments.size(); i++) {
            ProjectReqDTO.RecruitmentDTO dto = recruitments.get(i);
            ProjectRequirementMember requirement = requirementEntities.get(i);

            if (dto.techStacks() == null || dto.techStacks().isEmpty()) {
                continue;
            }

            TechName parentName = TechName.valueOf(dto.position().name());
            for (TechName techName : dto.techStacks()) {
                Techstack techstack = techstackRepository.findByNameAndParentStackName(techName, parentName)
                        .orElseThrow(() -> new TechstackException(TechstackErrorCode.NOT_FOUND));

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

    private void saveProjectImages(List<Long> imageIds, Project project, Long memberId) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }

        List<Image> images = imageRepository.findAllById(imageIds);
        if (images.size() != imageIds.size()) {
            throw new ImageException(ImageErrorCode.IMAGE_NOT_FOUND);
        }

        boolean hasUnauthorized = images.stream()
                .anyMatch(image -> image.getUploader() == null || !image.getUploader().getId().equals(memberId));
        if (hasUnauthorized) {
            throw new ImageException(ImageErrorCode.IMAGE_ACCESS_DENIED);
        }

        boolean hasNonProjectType = images.stream()
                .anyMatch(image -> image.getImageType() != ImageType.PROJECT);
        if (hasNonProjectType) {
            throw new ImageException(ImageErrorCode.IMAGE_TYPE_MISMATCH);
        }

        boolean hasUnuploaded = images.stream().anyMatch(image -> !image.isUploaded());
        if (hasUnuploaded) {
            throw new ImageException(ImageErrorCode.IMAGE_NOT_UPLOADED);
        }

        List<ProjectImage> projectImages = images.stream()
                .map(image -> ProjectConverter.toProjectImageFromImage(image, project))
                .collect(Collectors.toList());

        projectImageRepository.saveAll(projectImages).forEach(project::addImage);
    }

    @Override
    public void changeProjectStatus(Member member, Long projectId, ProjectStatus status) {
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        validateOwner(project, member.getId());

        switch (status) {
            case RECRUITING -> project.startRecruiting();
            case IN_PROGRESS -> project.startProgress();
            case COMPLETED -> project.complete();
            default -> throw new ProjectException(INVALID_STATUS_TRANSITION);
        }
    }

    private void validateOwner(Project project, Long memberId) {
        if (!project.getMember().getId().equals(memberId)) {
            throw new ProjectException(FORBIDDEN_PROJECT_ACCESS);
        }
    }

    private void validateRecruitmentDeadline(LocalDate recruitmentDeadline) {
        if (recruitmentDeadline.isBefore(LocalDate.now())) {
            throw new ProjectException(INVALID_RECRUITMENT_DEADLINE);
        }
    }

}
