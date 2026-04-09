package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.project.entity.Project;
import com.umc.devine.domain.project.entity.mapping.Matching;
import com.umc.devine.domain.project.enums.ProjectStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingDecision;
import com.umc.devine.domain.project.enums.mapping.MatchingStatus;
import com.umc.devine.domain.project.enums.mapping.MatchingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchingRepository extends JpaRepository<Matching, Long> {

    @Query("SELECT m FROM Matching m JOIN FETCH m.project JOIN FETCH m.member " +
            "WHERE m.project = :project AND m.member = :member AND m.matchingType = :matchingType AND m.status <> :excludeStatus")
    Optional<Matching> findByProjectAndMemberAndMatchingTypeAndStatusNot(
            @Param("project") Project project,
            @Param("member") Member member,
            @Param("matchingType") MatchingType matchingType,
            @Param("excludeStatus") MatchingStatus excludeStatus);

    @Query("SELECT COUNT(m) > 0 FROM Matching m " +
            "WHERE m.project = :project AND m.member = :member AND m.matchingType = :matchingType AND m.status <> :excludeStatus")
    boolean existsByProjectAndMemberAndMatchingTypeAndStatusNot(
            @Param("project") Project project,
            @Param("member") Member member,
            @Param("matchingType") MatchingType matchingType,
            @Param("excludeStatus") MatchingStatus excludeStatus);

    @Query("SELECT m FROM Matching m " +
            "JOIN FETCH m.project p " +
            "JOIN FETCH p.member " +
            "JOIN FETCH m.member " +
            "WHERE m.id = :matchingId")
    Optional<Matching> findByIdWithDetails(@Param("matchingId") Long matchingId);

    // PM용: 본인의 프로젝트들에 지원한/제안받은 개발자 목록 조회
    @Query(value = "SELECT m FROM Matching m " +
            "JOIN FETCH m.project p " +
            "JOIN FETCH m.member dev " +
            "WHERE p.member = :pm " +
            "AND m.matchingType = :matchingType " +
            "AND m.status <> :excludeStatus " +
            "ORDER BY m.createdAt DESC",
            countQuery = "SELECT COUNT(m) FROM Matching m " +
            "WHERE m.project.member = :pm " +
            "AND m.matchingType = :matchingType " +
            "AND m.status <> :excludeStatus")
    Page<Matching> findByProjectOwnerAndMatchingType(
            @Param("pm") Member pm,
            @Param("matchingType") MatchingType matchingType,
            @Param("excludeStatus") MatchingStatus excludeStatus,
            Pageable pageable);

    // 개발자용: 본인이 받은 제안/지원한 프로젝트 목록 조회
    @Query(value = "SELECT m FROM Matching m " +
            "JOIN FETCH m.project p " +
            "JOIN FETCH p.member pm " +
            "JOIN FETCH p.category " +
            "WHERE m.member = :developer " +
            "AND m.matchingType = :matchingType " +
            "AND m.status <> :excludeStatus " +
            "ORDER BY m.createdAt DESC",
            countQuery = "SELECT COUNT(m) FROM Matching m " +
            "WHERE m.member = :developer " +
            "AND m.matchingType = :matchingType " +
            "AND m.status <> :excludeStatus")
    Page<Matching> findByMemberAndMatchingType(
            @Param("developer") Member developer,
            @Param("matchingType") MatchingType matchingType,
            @Param("excludeStatus") MatchingStatus excludeStatus,
            Pageable pageable);

    // 내 프로젝트 조회 - 개발자 분기: 수락된 매칭 기반 프로젝트 상태별 조회 (페이징)
    @Query(value = "SELECT m FROM Matching m " +
            "JOIN FETCH m.project p " +
            "JOIN FETCH p.member pm " +
            "JOIN FETCH p.category " +
            "WHERE m.member = :developer " +
            "AND m.decision = :decision " +
            "AND p.status IN :projectStatuses " +
            "ORDER BY m.createdAt DESC",
            countQuery = "SELECT COUNT(m) FROM Matching m " +
            "JOIN m.project p " +
            "WHERE m.member = :developer " +
            "AND m.decision = :decision " +
            "AND p.status IN :projectStatuses")
    Page<Matching> findByMemberAndDecisionAndProjectStatusIn(
            @Param("developer") Member developer,
            @Param("decision") MatchingDecision decision,
            @Param("projectStatuses") List<ProjectStatus> projectStatuses,
            Pageable pageable);

    // 내 프로젝트 조회 - 수락된 매칭 기반 전체 조회 (비페이징, 내 프로젝트 통합 조회용)
    @Query("SELECT m FROM Matching m " +
            "JOIN FETCH m.project p " +
            "JOIN FETCH p.member pm " +
            "JOIN FETCH p.category " +
            "WHERE m.member = :developer " +
            "AND m.decision = :decision " +
            "AND p.status IN :projectStatuses " +
            "ORDER BY m.createdAt DESC")
    List<Matching> findAllByMemberAndDecisionAndProjectStatusIn(
            @Param("developer") Member developer,
            @Param("decision") MatchingDecision decision,
            @Param("projectStatuses") List<ProjectStatus> projectStatuses);

    // 특정 프로젝트-회원-타입 매칭 조회 (CANCELLED 제외)
    @Query("SELECT m FROM Matching m " +
            "JOIN FETCH m.project p " +
            "JOIN FETCH m.member mem " +
            "WHERE p.id = :projectId " +
            "AND mem.id = :memberId " +
            "AND m.matchingType = :matchingType " +
            "AND m.status <> :excludeStatus")
    Optional<Matching> findByProjectIdAndMemberIdAndTypeAndStatusNot(
            @Param("projectId") Long projectId,
            @Param("memberId") Long memberId,
            @Param("matchingType") MatchingType matchingType,
            @Param("excludeStatus") MatchingStatus excludeStatus);
}
