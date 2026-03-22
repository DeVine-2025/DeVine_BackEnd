package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.techstack.entity.mapping.ProjectTechstack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTechstackRepository extends JpaRepository<ProjectTechstack, Long> {
    List<ProjectTechstack> findByProject(Project project);
    List<ProjectTechstack> findByProjectId(Long projectId);
    void deleteAllByProject(Project project);
}