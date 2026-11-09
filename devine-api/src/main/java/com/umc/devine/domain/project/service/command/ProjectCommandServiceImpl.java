package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.category.entity.Category;
import com.umc.devine.domain.category.exception.CategoryException;
import com.umc.devine.domain.category.repository.CategoryRepository;
import com.umc.devine.domain.image.entity.Image;
import com.umc.devine.domain.image.enums.ImageType;
import com.umc.devine.domain.image.exception.ImageException;
import com.umc.devine.domain.image.exception.code.ImageErrorReason;
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
import com.umc.devine.domain.project.helper.ProjectDeletionHelper;
import com.umc.devine.domain.project.helper.ProjectLifecycleHelper;
import com.umc.devine.domain.project.repository.ProjectImageRepository;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.project.repository.ProjectRequirementMemberRepository;
import com.umc.devine.domain.techstack.entity.Techstack;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import com.umc.devine.domain.techstack.enums.TechName;
import com.umc.devine.domain.techstack.exception.TechstackException;
import com.umc.devine.domain.techstack.exception.code.TechstackErrorReason;
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
import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.umc.devine.domain.category.exception.code.CategoryErrorReason.CATEGORY_NOT_FOUND;
import static com.umc.devine.domain.project.exception.code.ProjectErrorReason.*;

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
    private final ProjectLifecycleHelper lifecycleHelper;
    private final ProjectDeletionHelper projectDeletionHelper;

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

        Map<Long, List<ProjectRequirementTechstack>> techstackMap =
                projectRequirementTechstackRepository.findAllByProjectIdsWithTechstack(List.of(savedProject.getId()))
                        .stream()
                        .collect(Collectors.groupingBy(prt -> prt.getRequirement().getId()));
        return ProjectConverter.toCreateProjectRes(savedProject, techstackMap);
    }

    @Override
    public ProjectResDTO.UpdateProjectRes updateProject(Member member, Long projectId, ProjectReqDTO.UpdateProjectReq request) {
        // 숨긴 프로젝트도 작성자는 수정할 수 있다(숨김 중 마감일 연장 포함)
        Project project = getOwnedProject(member, projectId);

        Category category = categoryRepository.findByGenre(request.category())
                .orElseThrow(() -> new CategoryException(CATEGORY_NOT_FOUND));

        // 마감일을 바꾼 경우에만 검사한다. 마감일이 지난 프로젝트의 소개만 고치는 것은 허용한다.
        boolean deadlineChanged = !request.recruitmentDeadline().equals(project.getRecruitmentDeadline());
        if (deadlineChanged) {
            validateRecruitmentDeadline(request.recruitmentDeadline());
            // 마감일을 오늘 이후로 늘리면 다시 모집한다
            if (project.getStatus() == ProjectStatus.DEADLINE_PASSED || project.getStatus() == ProjectStatus.CLOSED) {
                project.startRecruiting();
            }
        }

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

        Map<Long, List<ProjectRequirementTechstack>> techstackMap =
                projectRequirementTechstackRepository.findAllByProjectIdsWithTechstack(List.of(project.getId()))
                        .stream()
                        .collect(Collectors.groupingBy(prt -> prt.getRequirement().getId()));
        return ProjectConverter.toUpdateProjectRes(project, techstackMap);
    }

    @Override
    public void closeProject(Member member, Long projectId) {
        Project project = getOwnedProject(member, projectId);

        if (project.getStatus() == ProjectStatus.CLOSED) {
            return;
        }
        validateNoPending(project);

        project.close(member, LocalDateTime.now());
    }

    @Override
    public void changeVisibility(Member member, Long projectId, boolean hidden) {
        Project project = getOwnedProject(member, projectId);

        // 관리자 숨김은 신고 처리 결과라 PM이 풀 수 없다. 숨김 요청도 처리자를 PM으로 덮어쓰지 않도록 그대로 둔다.
        if (project.isHiddenByAdmin()) {
            if (!hidden) {
                throw new ProjectException(HIDDEN_BY_ADMIN);
            }
            return;
        }

        project.changeVisibility(!hidden, member, LocalDateTime.now());
    }

    @Override
    public void deleteProject(Member member, Long projectId) {
        Project project = getOwnedProject(member, projectId);

        if (project.getStatus() != ProjectStatus.CLOSED) {
            throw new ProjectException(NOT_DELETABLE);
        }
        validateNoPending(project);

        projectDeletionHelper.delete(project);
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
                        .orElseThrow(() -> new TechstackException(TechstackErrorReason.NOT_FOUND));

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
            throw new ImageException(ImageErrorReason.IMAGE_NOT_FOUND);
        }

        boolean hasUnauthorized = images.stream()
                .anyMatch(image -> image.getUploader() == null || !image.getUploader().getId().equals(memberId));
        if (hasUnauthorized) {
            throw new ImageException(ImageErrorReason.IMAGE_ACCESS_DENIED);
        }

        boolean hasNonProjectType = images.stream()
                .anyMatch(image -> image.getImageType() != ImageType.PROJECT);
        if (hasNonProjectType) {
            throw new ImageException(ImageErrorReason.IMAGE_TYPE_MISMATCH);
        }

        boolean hasUnuploaded = images.stream().anyMatch(image -> !image.isUploaded());
        if (hasUnuploaded) {
            throw new ImageException(ImageErrorReason.IMAGE_NOT_UPLOADED);
        }

        List<ProjectImage> projectImages = images.stream()
                .map(image -> ProjectConverter.toProjectImageFromImage(image, project))
                .collect(Collectors.toList());

        projectImageRepository.saveAll(projectImages).forEach(project::addImage);
    }

    @Override
    public void changeProjectStatus(Member member, Long projectId, ProjectStatus status) {
        Project project = getOwnedProject(member, projectId);

        // 모집 단계를 벗어나는 전환은 마감과 같은 조건(대기 건 0건)을 따른다
        if (project.isInRecruitingPhase() && status != ProjectStatus.RECRUITING) {
            validateNoPending(project);
        }

        // 마감일이 지났으면 상태만 되돌려 모집할 수 없다. 마감일 수정으로 다시 모집한다.
        if (status == ProjectStatus.RECRUITING) {
            validateRecruitmentDeadline(project.getRecruitmentDeadline());
        }

        switch (status) {
            case RECRUITING -> project.startRecruiting();
            case IN_PROGRESS -> project.startProgress();
            case COMPLETED -> project.complete();
            default -> throw new ProjectException(INVALID_STATUS_TRANSITION);
        }
    }

    private Project getOwnedProject(Member member, Long projectId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .filter(p -> p.getStatus() != ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));
        validateOwner(project, member.getId());
        return project;
    }

    private void validateNoPending(Project project) {
        if (lifecycleHelper.countPending(project) > 0) {
            throw new ProjectException(PENDING_MATCHINGS_EXIST);
        }
    }

    private void validateOwner(Project project, Long memberId) {
        if (!project.getMember().getId().equals(memberId)) {
            throw new ProjectException(FORBIDDEN_PROJECT_ACCESS);
        }
    }

    private void validateRecruitmentDeadline(LocalDate recruitmentDeadline) {
        if (recruitmentDeadline.isBefore(ProjectLifecycleHelper.today())) {
            throw new ProjectException(INVALID_RECRUITMENT_DEADLINE);
        }
    }

}
