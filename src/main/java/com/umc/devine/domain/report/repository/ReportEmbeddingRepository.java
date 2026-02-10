package com.umc.devine.domain.report.repository;

import com.umc.devine.domain.report.entity.ReportEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportEmbeddingRepository extends JpaRepository<ReportEmbedding, Long> {

    Optional<ReportEmbedding> findByDevReportId(Long devReportId);

    boolean existsByDevReportId(Long devReportId);
}
