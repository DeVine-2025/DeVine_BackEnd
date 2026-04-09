package com.umc.devine.domain.report.repository;

import com.umc.devine.domain.report.entity.ReportEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportEmbeddingRepository extends JpaRepository<ReportEmbedding, Long> {

    Optional<ReportEmbedding> findByDevReportId(Long devReportId);

    boolean existsByDevReportId(Long devReportId);

    // 개발자의 최신 성공 상태 리포트 임베딩을 조회
    @Query(value = """
        SELECT re.* FROM report_embedding re
        JOIN dev_report dr ON re.dev_report_id = dr.dev_report_id
        JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
        WHERE gru.member_id = :memberId
          AND re.status = 'SUCCESS'
          AND re.embedding IS NOT NULL
        ORDER BY re.created_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<ReportEmbedding> findLatestByMemberId(@Param("memberId") Long memberId);
}
