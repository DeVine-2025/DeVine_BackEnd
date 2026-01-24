package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.querydsl.ProjectQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectQueryDsl {
    List<Project> findByMember(Member member);

    Optional<Project> findByIdAndStatusNot(Long id, ProjectStatus status);

    // 주간 베스트 프로젝트 조회
    // - 월요일: previousWeekViewCount 기준 (전주 완성 데이터, 초반 데이터 부족 방지)
    // - 화~일: weeklyViewCount 기준 (이번 주 월요일부터 쌓인 데이터)
    @Query("SELECT p FROM Project p " +
            "WHERE p.status <> :status " +
            "ORDER BY CASE WHEN :isMonday = true THEN p.previousWeekViewCount ELSE p.weeklyViewCount END DESC, " +
            "p.createdAt DESC")
    List<Project> findWeeklyBestProjects(@Param("status") ProjectStatus status, @Param("isMonday") boolean isMonday);

    // 주간 조회수 리셋이 필요한 프로젝트 조회
    @Query("SELECT p FROM Project p " +
            "WHERE p.lastViewResetDate IS NULL " +
            "OR p.lastViewResetDate < :currentMonday")
    List<Project> findProjectsNeedingWeeklyReset(@Param("currentMonday") java.time.LocalDate currentMonday);

    // 조회수 원자적 증가 (동시성 안전)
    @Modifying
    @Query("UPDATE Project p SET p.totalViewCount = p.totalViewCount + 1, " +
            "p.weeklyViewCount = p.weeklyViewCount + 1 " +
            "WHERE p.id = :projectId")
    void incrementViewCount(@Param("projectId") Long projectId);

    // 주간 조회수 회전 (weeklyViewCount → previousWeekViewCount, weeklyViewCount = 0)
    @Modifying
    @Query("UPDATE Project p SET p.previousWeekViewCount = p.weeklyViewCount, " +
            "p.weeklyViewCount = 0, " +
            "p.lastViewResetDate = :resetDate " +
            "WHERE p.lastViewResetDate IS NULL OR p.lastViewResetDate < :resetDate")
    int rotateWeeklyViewCount(@Param("resetDate") java.time.LocalDate resetDate);
}