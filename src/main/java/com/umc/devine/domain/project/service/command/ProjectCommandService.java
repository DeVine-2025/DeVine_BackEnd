package com.umc.devine.domain.project.service.command;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.dto.ProjectReqDTO;
import com.umc.devine.domain.project.dto.ProjectResDTO;

public interface ProjectCommandService {

    ProjectResDTO.CreateProjectRes createProject(Member member, ProjectReqDTO.CreateProjectReq request);

    ProjectResDTO.UpdateProjectRes updateProject(Member member, Long projectId, ProjectReqDTO.UpdateProjectReq request);

    void deleteProject(Member member, Long projectId);
}
