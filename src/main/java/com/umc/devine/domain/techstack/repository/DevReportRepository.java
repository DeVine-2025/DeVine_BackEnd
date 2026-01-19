package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.member.entity.GitRepoUrl;
import com.umc.devine.domain.techstack.entity.DevReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DevReportRepository extends JpaRepository<DevReport, Long> {
    Optional<DevReport> findByGitRepoUrl(GitRepoUrl gitRepoUrl);
    List<DevReport> findAllByGitRepoUrlIn(List<GitRepoUrl> gitRepoUrls);
}
