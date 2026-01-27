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

    boolean existsByGitRepoUrl_IdAndReportType(Long gitRepoId, ReportType reportType);

    @Query("SELECT r FROM DevReport r " +
            "JOIN FETCH r.gitRepoUrl " +
            "WHERE r.gitRepoUrl.id = :gitRepoId AND r.reportType = :reportType")
    Optional<DevReport> findByGitRepoIdAndReportType(
            @Param("gitRepoId") Long gitRepoId,
            @Param("reportType") ReportType reportType
    );
}
