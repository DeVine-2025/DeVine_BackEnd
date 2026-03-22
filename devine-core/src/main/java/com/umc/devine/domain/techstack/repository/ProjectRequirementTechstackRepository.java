package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.project.entity.ProjectRequirementMember;
import com.umc.devine.domain.techstack.entity.mapping.ProjectRequirementTechstack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRequirementTechstackRepository extends JpaRepository<ProjectRequirementTechstack, Long> {
    void deleteAllByRequirementIn(List<ProjectRequirementMember> requirements);

    @Query("SELECT prt FROM ProjectRequirementTechstack prt " +
           "JOIN FETCH prt.techstack " +
           "JOIN prt.requirement prm " +
           "WHERE prm.project.id = :projectId")
    List<ProjectRequirementTechstack> findAllByProjectIdWithTechstack(@Param("projectId") Long projectId);

    @Query("SELECT prt FROM ProjectRequirementTechstack prt " +
           "JOIN FETCH prt.techstack " +
           "JOIN FETCH prt.requirement prm " +
           "WHERE prm.project.id IN :projectIds")
    List<ProjectRequirementTechstack> findAllByProjectIdsWithTechstack(@Param("projectIds") List<Long> projectIds);
}