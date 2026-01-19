package com.umc.devine.domain.project.dto;

import com.umc.devine.domain.project.enums.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class ProjectResDTO {

    // TODO: 내 프로젝트 조회용 DTO 추후 통합 할 것
    @Builder
    @Getter
    public static class ProjectDetailDTO {
        private Long id;
        private String name;
        private String content;
        private ProjectStatus status;
        private List<String> imageUrls;
    }

    // TODO: 내 프로젝트 조회용 DTO 추후 통합 할 것
    @Builder
    @Getter
    public static class ProjectListDTO {
        private List<ProjectDetailDTO> projects;
    }
}
