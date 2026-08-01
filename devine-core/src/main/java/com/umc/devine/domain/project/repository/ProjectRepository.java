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
    // PM용: 본인 프로젝트를 상태별로 조회 (페이징)
    // 비노출 프로젝트는 제외한다. 제3자에게 보이는 경로에서도 쓰일 수 있으므로 안전한 쪽을 기본값으로 둔다.
    @Query(value = "SELECT p FROM Project p " +
            "JOIN FETCH p.category " +
            "WHERE p.member = :member " +
            "AND p.status IN :statuses " +
            "AND p.hidden = false " +
            "ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Project p " +
            "WHERE p.member = :member " +
            "AND p.status IN :statuses " +
            "AND p.hidden = false")
    Page<Project> findByMemberAndStatusIn(
            @Param("member") Member member,
            @Param("statuses") List<ProjectStatus> statuses,
            Pageable pageable);

    // 회원의 프로젝트를 상태별로 전체 조회 (비페이징)
    // 비노출 프로젝트는 제외한다. 공개 프로필(GET /members/{nickname}/projects)처럼 제3자가 보는 경로에서
    // 재사용되므로, 비노출 프로젝트를 포함하려면 아래 IncludingHidden 메서드를 명시적으로 호출해야 한다.
    @Query("SELECT p FROM Project p JOIN FETCH p.category WHERE p.member = :member AND p.status IN :statuses AND p.hidden = false ORDER BY p.createdAt DESC")
    List<Project> findAllByMemberAndStatusIn(
            @Param("member") Member member,
            @Param("statuses") List<ProjectStatus> statuses);

    // 작성자 본인의 "내 프로젝트" 목록 전용: 비노출 프로젝트까지 포함한다.
    // 소유자는 자기 글이 비노출됐는지 확인할 수 있어야 하므로 이 경로에서만 사용한다.
    // 제3자에게 노출되는 경로에서 절대 호출하면 안 된다.
    @Query("SELECT p FROM Project p JOIN FETCH p.category WHERE p.member = :member AND p.status IN :statuses ORDER BY p.createdAt DESC")
    List<Project> findAllByMemberAndStatusInIncludingHidden(
            @Param("member") Member member,
            @Param("statuses") List<ProjectStatus> statuses);

    // 유저 화면에 노출 가능한 프로젝트 단건 조회 (삭제되지 않고 비노출 처리도 되지 않은 것)
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED AND p.hidden = false")
    Optional<Project> findVisibleById(@Param("id") Long id);

    // 프로젝트 상세 조회용 (Member, Category fetch join)
    @Query("SELECT p FROM Project p JOIN FETCH p.member JOIN FETCH p.category " +
            "WHERE p.id = :id AND p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED AND p.hidden = false")
    Optional<Project> findVisibleByIdWithMember(@Param("id") Long id);

    // 상세 조회용: 비노출 프로젝트는 작성자 본인에게만 보인다.
    // viewerMemberId가 null(비로그인)이면 노출된 프로젝트만 조회된다.
    @Query("SELECT p FROM Project p JOIN FETCH p.member JOIN FETCH p.category " +
            "WHERE p.id = :id " +
            "AND p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED " +
            "AND (p.hidden = false OR p.member.id = :viewerMemberId)")
    Optional<Project> findByIdWithMemberVisibleTo(@Param("id") Long id, @Param("viewerMemberId") Long viewerMemberId);

    @Query("SELECT p FROM Project p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Project> findByIdWithCategory(@Param("id") Long id);

    // 관리자 목록 조회용: 삭제된 프로젝트는 노출 전환 대상이 아니므로 제외한다.
    // hidden이 null이면 노출/비노출 전체 조회.
    @Query(value = "SELECT p FROM Project p " +
            "JOIN FETCH p.member " +
            "WHERE p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED " +
            "AND (:hidden IS NULL OR p.hidden = :hidden) " +
            "ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Project p " +
            "WHERE p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED " +
            "AND (:hidden IS NULL OR p.hidden = :hidden)")
    Page<Project> findForAdmin(@Param("hidden") Boolean hidden, Pageable pageable);

    // 주간 베스트 프로젝트 조회
    // - 월요일: previousWeekViewCount 기준 (전주 완성 데이터, 초반 데이터 부족 방지)
    // - 화~일: weeklyViewCount 기준 (이번 주 월요일부터 쌓인 데이터)
    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.category " +
            "JOIN FETCH p.member " +
            "WHERE p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED " +
            "AND p.hidden = false " +
            "AND p.recruitmentDeadline >= CURRENT_DATE " +
            "ORDER BY CASE WHEN :isMonday = true THEN p.previousWeekViewCount ELSE p.weeklyViewCount END DESC, " +
            "p.createdAt DESC")
    List<Project> findWeeklyBestProjects(@Param("isMonday") boolean isMonday);

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

    // 유저 화면에 노출 가능한 프로젝트 다건 조회
    // (N+1 방지용 IN 쿼리, 추천 프로젝트 조회 - List 컬렉션 동시 FETCH 불가하므로 category/member만)
    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.member " +
            "WHERE p.id IN :ids " +
            "AND p.status <> com.umc.devine.domain.project.enums.ProjectStatus.DELETED " +
            "AND p.hidden = false")
    List<Project> findVisibleByIdIn(@Param("ids") List<Long> ids);

    // 기본 추천용: 최신 모집 중 프로젝트 조회
    @Query("SELECT p FROM Project p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.member " +
            "WHERE p.status = :status " +
            "AND p.hidden = false " +
            "AND p.recruitmentDeadline >= CURRENT_DATE " +
            "ORDER BY p.createdAt DESC")
    List<Project> findByStatusOrderByCreatedAtDesc(@Param("status") ProjectStatus status);
}