package com.umc.devine.domain.member.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class MemberRecommendRepository {

    private final EntityManager entityManager;

    /**
     * 프로젝트에 적합한 개발자를 복합 점수 기반으로 추천합니다.
     *
     * 복합 점수 (100점 만점):
     * 1. 리포트 유사도: 60점 - 프로젝트 임베딩 ↔ 개발자 리포트 임베딩 코사인 유사도 × 60
     * 2. 기술 스택 매칭: 20점 - (매칭 개수 / 프로젝트 요구 개수) × 20
     * 3. 도메인 일치도: 20점 - 기본 10점 + 도메인 일치 시 +10점
     *
     * 결과 Object[] 구조:
     * [0] member_id (Long)
     * [1] similarity_score (Double, 60점 만점)
     * [2] techstack_score (Double, 20점 만점)
     * [3] domain_score (Integer, 20점 만점)
     * [4] total_score (Double, 100점 만점)
     * [5] similarity_score_percent (Double, 100점 만점)
     * [6] techstack_score_percent (Double, 100점 만점)
     * [7] domain_match (Boolean)
     *
     * @param projectId 프로젝트 ID
     * @param limit 조회할 개발자 수
     * @param offset 시작 위치
     * @return 추천 개발자 목록 (점수 정보 포함)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findRecommendedDevelopers(Long projectId, Long currentMemberId, int limit, int offset) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("currentMemberId", currentMemberId);
        params.put("limit", limit);
        params.put("offset", offset);

        String sql = """
            WITH project_open_positions AS (
                -- 프로젝트가 요구하는 모집 중인 포지션
                SELECT DISTINCT prm.req_mem_part AS position
                FROM project_requirement_member prm
                WHERE prm.project_id = :projectId
                  AND prm.current_count < prm.req_mem_num
            ),
            project_required_techstacks AS (
                -- 프로젝트가 요구하는 기술스택
                SELECT DISTINCT t.techstack_id, t.techstack_name
                FROM project_requirement_techstack prt
                JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
                JOIN techstack t ON prt.techstack_id = t.techstack_id
                WHERE prm.project_id = :projectId
            ),
            dev_latest_embedding AS (
                -- 각 개발자의 최신 성공 상태 리포트 임베딩
                SELECT DISTINCT ON (gru.member_id)
                    gru.member_id,
                    re.report_embedding_id,
                    re.embedding
                FROM report_embedding re
                JOIN dev_report dr ON re.dev_report_id = dr.dev_report_id
                JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
                WHERE re.status = 'SUCCESS'
                  AND re.embedding IS NOT NULL
                ORDER BY gru.member_id, re.created_at DESC
            ),
            scores AS (
                SELECT
                    m.member_id,
                    -- 코사인 유사도 (프로젝트 ↔ 개발자 리포트)
                    GREATEST(0, 1 - (pe.embedding <=> dle.embedding)) AS cosine_sim,
                    -- 기술스택 매칭률
                    COALESCE(
                        (SELECT COUNT(DISTINCT dt.techstack_id)::float
                         FROM dev_techstack dt
                         WHERE dt.member_id = m.member_id
                           AND dt.techstack_id IN (SELECT techstack_id FROM project_required_techstacks)
                        ) / NULLIF(
                            (SELECT COUNT(*)::float FROM project_required_techstacks), 0
                        ), 0
                    ) AS techstack_ratio,
                    -- 도메인 일치 여부
                    EXISTS (
                        SELECT 1 FROM member_category mc
                        JOIN project p ON p.domain_id = mc.category_id
                        WHERE mc.member_id = m.member_id AND p.project_id = :projectId
                    ) AS domain_match
                FROM project_embedding pe
                JOIN project p ON pe.project_id = p.project_id
                JOIN dev_latest_embedding dle ON TRUE
                JOIN member m ON dle.member_id = m.member_id
                WHERE pe.project_id = :projectId
                  AND pe.status = 'SUCCESS'
                  AND m.disclosure = true
                  AND m.used = 'ACTIVE'
                  AND m.member_id != :currentMemberId
                  -- 개발자의 루트 포지션이 프로젝트 모집 포지션과 일치
                  AND EXISTS (
                    SELECT 1 FROM dev_techstack dt
                    JOIN techstack t ON dt.techstack_id = t.techstack_id
                    LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
                    WHERE dt.member_id = m.member_id
                      AND (
                        (t.parent_stack IS NULL AND t.techstack_name IN (SELECT position FROM project_open_positions))
                        OR (root.parent_stack IS NULL AND root.techstack_name IN (SELECT position FROM project_open_positions))
                      )
                  )
            )
            SELECT
                member_id,
                cosine_sim * 60                                          AS similarity_score,
                techstack_ratio * 20                                     AS techstack_score,
                CASE WHEN domain_match THEN 20 ELSE 10 END               AS domain_score,
                cosine_sim * 60 + techstack_ratio * 20
                    + CASE WHEN domain_match THEN 20 ELSE 10 END         AS total_score,
                cosine_sim * 100                                         AS similarity_score_percent,
                techstack_ratio * 100                                    AS techstack_score_percent,
                domain_match
            FROM scores
            ORDER BY total_score DESC
            LIMIT :limit OFFSET :offset
            """;

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        return query.getResultList();
    }

    /**
     * 프리뷰용 추천 개발자 조회 (offset 없음)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findRecommendedDevelopersPreview(Long projectId, Long currentMemberId, int limit) {
        return findRecommendedDevelopers(projectId, currentMemberId, limit, 0);
    }

    /**
     * 벡터 검색으로 추천 가능한 개발자 총 수 조회
     */
    public long countRecommendedDevelopers(Long projectId, Long currentMemberId) {
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        params.put("currentMemberId", currentMemberId);

        String sql = """
            WITH project_open_positions AS (
                SELECT DISTINCT prm.req_mem_part AS position
                FROM project_requirement_member prm
                WHERE prm.project_id = :projectId
                  AND prm.current_count < prm.req_mem_num
            ),
            dev_latest_embedding AS (
                SELECT DISTINCT ON (gru.member_id)
                    gru.member_id,
                    re.report_embedding_id
                FROM report_embedding re
                JOIN dev_report dr ON re.dev_report_id = dr.dev_report_id
                JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
                WHERE re.status = 'SUCCESS'
                  AND re.embedding IS NOT NULL
                ORDER BY gru.member_id, re.created_at DESC
            )
            SELECT COUNT(DISTINCT m.member_id)
            FROM project_embedding pe
            JOIN project p ON pe.project_id = p.project_id
            JOIN dev_latest_embedding dle ON TRUE
            JOIN member m ON dle.member_id = m.member_id
            WHERE pe.project_id = :projectId
              AND pe.status = 'SUCCESS'
              AND m.disclosure = true
              AND m.used = 'ACTIVE'
              AND m.member_id != :currentMemberId
              AND EXISTS (
                SELECT 1 FROM dev_techstack dt
                JOIN techstack t ON dt.techstack_id = t.techstack_id
                LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
                WHERE dt.member_id = m.member_id
                  AND (
                    (t.parent_stack IS NULL AND t.techstack_name IN (SELECT position FROM project_open_positions))
                    OR (root.parent_stack IS NULL AND root.techstack_name IN (SELECT position FROM project_open_positions))
                  )
              )
            """;

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        return ((Number) query.getSingleResult()).longValue();
    }

    /**
     * 개발자의 매칭된 기술스택 목록 조회
     */
    @SuppressWarnings("unchecked")
    public List<String> findMatchedTechstacks(Long memberId, Long projectId) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("projectId", projectId);

        String sql = """
            SELECT t.techstack_name
            FROM dev_techstack dt
            JOIN techstack t ON dt.techstack_id = t.techstack_id
            WHERE dt.member_id = :memberId
              AND dt.techstack_id IN (
                SELECT prt.techstack_id
                FROM project_requirement_techstack prt
                JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
                WHERE prm.project_id = :projectId
              )
            """;

        Query query = entityManager.createNativeQuery(sql);
        params.forEach(query::setParameter);

        return query.getResultList();
    }
}
