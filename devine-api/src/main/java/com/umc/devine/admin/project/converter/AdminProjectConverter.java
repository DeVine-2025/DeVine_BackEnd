package com.umc.devine.admin.project.converter;

import com.umc.devine.admin.project.dto.AdminProjectResDTO;
import com.umc.devine.domain.project.entity.Project;

public class AdminProjectConverter {

    public static AdminProjectResDTO.ProjectSummaryDTO toProjectSummaryDTO(Project project) {
        return AdminProjectResDTO.ProjectSummaryDTO.builder()
                .projectId(project.getId())
                .title(project.getName())
                .authorNickname(project.getMember().getNickname())
                .createdAt(project.getCreatedAt())
                .visible(!project.isHidden())
                .build();
    }
}
