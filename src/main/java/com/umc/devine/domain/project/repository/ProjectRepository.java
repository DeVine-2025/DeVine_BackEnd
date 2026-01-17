package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.querydsl.ProjectQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectQueryDsl {
    Optional<Project> findByIdAndStatusNot(Long id, ProjectStatus status);

    // 이번 주(월~일) 생성된 프로젝트 중 주간 조회수가 높은 순으로 조회
    @Query("SELECT p FROM Project p " +
            "WHERE p.status <> :status " +
            "AND p.createdAt BETWEEN :startOfWeek AND :endOfWeek " +
            "ORDER BY p.weeklyViewCount DESC, p.createdAt DESC")
    List<Project> findWeeklyBestProjects(
            @Param("status") ProjectStatus status,
            @Param("startOfWeek") LocalDateTime startOfWeek,
            @Param("endOfWeek") LocalDateTime endOfWeek
    );

    // 주간 조회수 리셋이 필요한 프로젝트 조회
    @Query("SELECT p FROM Project p " +
            "WHERE p.lastViewResetDate IS NULL " +
            "OR p.lastViewResetDate < :currentMonday")
    List<Project> findProjectsNeedingWeeklyReset(@Param("currentMonday") java.time.LocalDate currentMonday);
}