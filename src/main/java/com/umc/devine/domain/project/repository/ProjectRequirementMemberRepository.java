package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRequirementMemberRepository extends JpaRepository<ProjectRequirementMember, Long> {
        List<ProjectRequirementMember> findByProject_Id(Long projectId);
        void deleteAllByProject(Project project);
        List<ProjectRequirementMember> findAllByProject(Project project);
}
