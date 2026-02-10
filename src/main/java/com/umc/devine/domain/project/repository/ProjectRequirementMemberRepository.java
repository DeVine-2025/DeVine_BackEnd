package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.project.enums.ProjectPart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRequirementMemberRepository extends JpaRepository<ProjectRequirementMember, Long> {
        void deleteAllByProject(Project project);
        List<ProjectRequirementMember> findAllByProject(Project project);
        Optional<ProjectRequirementMember> findByProjectAndPart(Project project, ProjectPart part);
}
