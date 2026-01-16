package com.umc.devine.domain.project.service.query;

import com.umc.devine.domain.project.dto.ProjectResDTO;

public interface ProjectQueryService {
    ProjectResDTO.UpdateProjectRes getProjectDetail(Long memberId, Long projectId);
}
