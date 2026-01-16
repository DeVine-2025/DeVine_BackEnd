package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {
    void deleteAllByProject(Project project);
    List<ProjectImage> findAllByProject(Project project);
}