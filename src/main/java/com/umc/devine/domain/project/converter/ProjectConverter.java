package com.umc.devine.domain.project.converter;

import com.umc.devine.domain.project.dto.ProjectResDTO;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectImage;

import java.util.List;
import java.util.stream.Collectors;

public class ProjectConverter {

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
