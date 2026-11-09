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

    @Query("SELECT m.project.id FROM Matching m WHERE m.id = :id")
    Optional<Long> findProjectIdById(@Param("id") Long id);

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
            "AND p.hidden = false " +
            "ORDER BY m.createdAt DESC",
            countQuery = "SELECT COUNT(m) FROM Matching m " +
            "JOIN m.project p " +
            "WHERE m.member = :developer " +
            "AND m.decision = :decision " +
            "AND p.status IN :projectStatuses " +
            "AND p.hidden = false")
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
            "AND p.hidden = false " +
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

    // 프로젝트 관리용: 대기 건(PENDING) 수와 목록. 마감·삭제 가능 여부 판단에 쓴다.
    long countByProjectAndStatus(Project project, MatchingStatus status);

    @Query("SELECT m FROM Matching m JOIN FETCH m.member " +
            "WHERE m.project = :project AND m.status = :status ORDER BY m.createdAt ASC")
    List<Matching> findAllByProjectAndStatusWithMember(
            @Param("project") Project project,
            @Param("status") MatchingStatus status);

    // 삭제 시 매칭 이력이 있으면 프로젝트 행(이름)을 남긴다
    boolean existsByProject(Project project);

    // 보관 만료 파기 배치용: 개발자 쪽이나 PM 쪽이 탈퇴한 매칭
    @Query("SELECT m FROM Matching m " +
            "JOIN FETCH m.project p " +
            "JOIN FETCH p.member pm " +
            "JOIN FETCH m.member dev " +
            "WHERE dev.used = com.umc.devine.domain.member.enums.MemberStatus.DELETED " +
            "OR pm.used = com.umc.devine.domain.member.enums.MemberStatus.DELETED")
    List<Matching> findAllWithWithdrawnParticipant();

    // 회원 탈퇴용: 이 회원이 보냈거나 받은 건(개발자 쪽 matching.member, PM 쪽 project.member)이 걸린 프로젝트 ID
    @Query("SELECT DISTINCT p.id FROM Matching m JOIN m.project p " +
            "WHERE m.status = :status AND (m.member = :member OR p.member = :member)")
    List<Long> findProjectIdsInvolvingMemberByStatus(
            @Param("member") Member member,
            @Param("status") MatchingStatus status);

    // 회원 탈퇴용: 이 회원이 보냈거나 받은 건(개발자 쪽 matching.member, PM 쪽 project.member)
    @Query("SELECT m FROM Matching m " +
            "JOIN FETCH m.project p " +
            "JOIN FETCH p.member " +
            "JOIN FETCH m.member " +
            "WHERE m.status = :status AND (m.member = :member OR p.member = :member) " +
            "ORDER BY m.createdAt ASC, m.id ASC")
    List<Matching> findAllInvolvingMemberByStatus(
            @Param("member") Member member,
            @Param("status") MatchingStatus status);
}
