package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;

public interface ProjectCommandService {

    ProjectResDTO.CreateProjectRes createProject(Long memberId, ProjectReqDTO.CreateProjectReq request);

    ProjectResDTO.UpdateProjectRes updateProject(Long memberId, Long projectId, ProjectReqDTO.UpdateProjectReq request);

    void deleteProject(Long memberId, Long projectId);
}
