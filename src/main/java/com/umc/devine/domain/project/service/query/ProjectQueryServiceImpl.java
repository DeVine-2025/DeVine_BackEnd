package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.project.converter.ProjectConverter;
import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.exception.ProjectException;
import com.umc.devine.domain.project.repository.ProjectRepository;
import com.umc.devine.domain.techstack.repository.ProjectRequirementTechstackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.umc.devine.domain.project.exception.code.ProjectErrorCode.PROJECT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectQueryServiceImpl implements ProjectQueryService {

    private final ProjectRepository projectRepository;
    private final ProjectRequirementTechstackRepository projectRequirementTechstackRepository;

    @Override
    public ProjectResDTO.UpdateProjectRes getProjectDetail(Long memberId, Long projectId) {
        // 소프트 삭제된 프로젝트는 조회되지 않음
        Project project = projectRepository.findByIdAndStatusNot(projectId, ProjectStatus.DELETED)
                .orElseThrow(() -> new ProjectException(PROJECT_NOT_FOUND));

        return ProjectConverter.toUpdateProjectRes(project, projectRequirementTechstackRepository);
    }
}
