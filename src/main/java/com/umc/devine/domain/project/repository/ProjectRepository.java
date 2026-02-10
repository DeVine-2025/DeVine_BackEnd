package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.repository.querydsl.ProjectQueryDsl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectQueryDsl {
    // PM용: 본인 프로젝트를 상태별로 조회
    @Query(value = "SELECT p FROM Project p " +
            "JOIN FETCH p.category " +
            "WHERE p.member = :member " +
            "AND p.status IN :statuses " +
            "ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Project p " +
            "WHERE p.member = :member " +
            "AND p.status IN :statuses")
    Page<Project> findByMemberAndStatusIn(
            @Param("member") Member member,
            @Param("statuses") List<ProjectStatus> statuses,
            Pageable pageable);

    Optional<Project> findByIdAndStatusNot(Long id, ProjectStatus status);

    @Query("SELECT p FROM Project p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Project> findByIdWithCategory(@Param("id") Long id);

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

    // N+1 방지용 IN 쿼리 (추천 프로젝트 조회 - List 컬렉션 동시 FETCH 불가하므로 category/member만)
    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.member " +
            "WHERE p.id IN :ids")
    List<Project> findAllByIdIn(@Param("ids") List<Long> ids);

    // 기본 추천용: 최신 모집 중 프로젝트 조회
    @Query("SELECT p FROM Project p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.member " +
            "WHERE p.status = :status " +
            "ORDER BY p.createdAt DESC")
    List<Project> findByStatusOrderByCreatedAtDesc(@Param("status") ProjectStatus status);
}