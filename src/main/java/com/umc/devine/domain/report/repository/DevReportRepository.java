package com.umc.devine.domain.report.repository;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.report.enums.ReportType;
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

    boolean existsByGitRepoUrlId(Long gitRepoId);
}