package com.umc.devine.domain.report.repository;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
import com.umc.devine.domain.report.enums.ReportVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DevReportRepository extends JpaRepository<DevReport, Long> {

    List<DevReport> findAllByGitRepoUrlIn(List<GitRepoUrl> gitRepoUrls);

    @Query("SELECT r FROM DevReport r " +
            "JOIN FETCH r.gitRepoUrl g " +
            "JOIN FETCH g.member " +
            "WHERE g.id = :gitRepoId AND r.reportType = :reportType")
    Optional<DevReport> findByGitRepoIdAndReportTypeWithMember(
            @Param("gitRepoId") Long gitRepoId,
            @Param("reportType") ReportType reportType
    );

    @Query("SELECT r FROM DevReport r " +
            "JOIN FETCH r.gitRepoUrl g " +
            "JOIN FETCH g.member " +
            "WHERE r.id = :reportId")
    Optional<DevReport> findByIdWithMember(@Param("reportId") Long reportId);


    /**
     * 실패하지 않은(성공 또는 진행 중인) 리포트가 존재하는지 확인
     */
    @Query("SELECT COUNT(r) > 0 FROM DevReport r WHERE r.gitRepoUrl.id = :gitRepoId AND r.errorMessage IS NULL")
    boolean existsActiveReportByGitRepoUrlId(@Param("gitRepoId") Long gitRepoId);

    boolean existsByGitRepoUrlId(Long gitRepoId);

    /**
     * 활성 리포트가 존재하는 gitRepoId 목록 반환 (배치 조회)
     */
    @Query("SELECT DISTINCT r.gitRepoUrl.id FROM DevReport r WHERE r.gitRepoUrl.id IN :gitRepoIds AND r.errorMessage IS NULL")
    List<Long> findActiveReportGitRepoIds(@Param("gitRepoIds") List<Long> gitRepoIds);

    @Query("SELECT r FROM DevReport r " +
            "JOIN FETCH r.gitRepoUrl g " +
            "WHERE g.member = :member " +
            "AND (:reportType IS NULL OR r.reportType = :reportType) " +
            "ORDER BY r.createdAt DESC")
    List<DevReport> findAllByMemberAndReportType(
            @Param("member") Member member,
            @Param("reportType") ReportType reportType
    );

    @Query(value = "SELECT r FROM DevReport r " +
            "JOIN FETCH r.gitRepoUrl g " +
            "WHERE g.member = :member " +
            "AND (:reportType IS NULL OR r.reportType = :reportType) " +
            "AND r.visibility = :visibility " +
            "ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM DevReport r " +
            "JOIN r.gitRepoUrl g " +
            "WHERE g.member = :member " +
            "AND (:reportType IS NULL OR r.reportType = :reportType) " +
            "AND r.visibility = :visibility")
    Page<DevReport> findAllByMemberAndReportTypeAndVisibility(
            @Param("member") Member member,
            @Param("reportType") ReportType reportType,
            @Param("visibility") ReportVisibility visibility,
            Pageable pageable
    );
}