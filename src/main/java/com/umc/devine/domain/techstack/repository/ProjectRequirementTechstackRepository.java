package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRequirementTechstackRepository extends JpaRepository<ProjectRequirementTechstack, Long> {
    List<ProjectRequirementTechstack> findByRequirement(ProjectRequirementMember requirement);
    void deleteAllByRequirementIn(List<ProjectRequirementMember> requirements);
}