package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.category.enums.CategoryGenre;
import com.umc.devine.domain.project.enums.DurationRange;
import com.umc.devine.domain.project.enums.ProjectField;
import com.umc.devine.domain.techstack.enums.TechName;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProjectRecommendRepository {

    private final EntityManager entityManager;

    /**
     * 개발자에게 적합한 프로젝트를 복합 점수 기반으로 추천합니다.
     *
     * 복합 점수 (100점 만점):
     * 1. 리포트 유사도: 60점 - 개발자 리포트 임베딩 ↔ 프로젝트 임베딩 코사인 유사도 × 60
     * 2. 기술 스택 매칭: 20점 - 개발자 루트 포지션 기준 (일치 개수 / 해당 포지션 요구 개수) × 20
     * 3. 도메인 일치도: 20점 - 기본 10점 + 도메인 일치 시 +10점
     *
     * 결과 Object[] 구조:
     * [0] project_id (Long)
     * [1] similarity_score (Double, 60점 만점)
     * [2] techstack_score (Double, 20점 만점)
     * [3] domain_score (Integer, 20점 만점)
     * [4] total_score (Double, 100점 만점)
     * [5] similarity_score_percent (Double, 100점 만점)
     * [6] techstack_score_percent (Double, 100점 만점)
     * [7] domain_match (Boolean)
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findRecommendedProjects(
            Long memberId,
            int limit,
            List<ProjectField> projectFields,
            List<CategoryGenre> categories,
            List<TechName> techstackNames,
            List<DurationRange> durationRanges
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("limit", limit);

        StringBuilder sql = new StringBuilder();
        sql.append("""
            WITH dev_root_positions AS (
                SELECT DISTINCT CASE
                    WHEN t.parent_stack IS NULL THEN t.techstack_name
                    ELSE root.techstack_name
                END AS root_position
                FROM dev_techstack dt
                JOIN techstack t ON dt.techstack_id = t.techstack_id
                LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
                WHERE dt.member_id = :memberId
                  AND (t.parent_stack IS NULL OR root.parent_stack IS NULL)
            )
            SELECT
                p.project_id,
                GREATEST(0, 1 - (re.embedding <=> pe.embedding)) * 60 AS similarity_score,
                COALESCE(
                    (SELECT COUNT(DISTINCT dt.techstack_id)::float
                     FROM dev_techstack dt
                     JOIN project_requirement_techstack prt ON dt.techstack_id = prt.techstack_id
                     JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
                     WHERE dt.member_id = :memberId AND prm.project_id = p.project_id
                       AND prm.req_mem_part IN (SELECT root_position FROM dev_root_positions)
                    ) / NULLIF(
                        (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                         FROM project_requirement_techstack prt2
                         JOIN project_requirement_member prm2 ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                         WHERE prm2.project_id = p.project_id
                           AND prm2.req_mem_part IN (SELECT root_position FROM dev_root_positions)
                        ), 0
                    ) * 20, 0
                ) AS techstack_score,
                CASE
                    WHEN EXISTS (
                        SELECT 1 FROM member_category mc
                        WHERE mc.member_id = :memberId AND mc.category_id = p.domain_id
                    ) THEN 20
                    ELSE 10
                END AS domain_score,
                GREATEST(0, 1 - (re.embedding <=> pe.embedding)) * 60 +
                COALESCE(
                    (SELECT COUNT(DISTINCT dt.techstack_id)::float
                     FROM dev_techstack dt
                     JOIN project_requirement_techstack prt ON dt.techstack_id = prt.techstack_id
                     JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
                     WHERE dt.member_id = :memberId AND prm.project_id = p.project_id
                       AND prm.req_mem_part IN (SELECT root_position FROM dev_root_positions)
                    ) / NULLIF(
                        (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                         FROM project_requirement_techstack prt2
                         JOIN project_requirement_member prm2 ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                         WHERE prm2.project_id = p.project_id
                           AND prm2.req_mem_part IN (SELECT root_position FROM dev_root_positions)
                        ), 0
                    ) * 20, 0
                ) +
                CASE
                    WHEN EXISTS (
                        SELECT 1 FROM member_category mc
                        WHERE mc.member_id = :memberId AND mc.category_id = p.domain_id
                    ) THEN 20
                    ELSE 10
                END AS total_score,
                GREATEST(0, 1 - (re.embedding <=> pe.embedding)) * 100 AS similarity_score_percent,
                COALESCE(
                    (SELECT COUNT(DISTINCT dt.techstack_id)::float
                     FROM dev_techstack dt
                     JOIN project_requirement_techstack prt ON dt.techstack_id = prt.techstack_id
                     JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
                     WHERE dt.member_id = :memberId AND prm.project_id = p.project_id
                       AND prm.req_mem_part IN (SELECT root_position FROM dev_root_positions)
                    ) / NULLIF(
                        (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                         FROM project_requirement_techstack prt2
                         JOIN project_requirement_member prm2 ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                         WHERE prm2.project_id = p.project_id
                           AND prm2.req_mem_part IN (SELECT root_position FROM dev_root_positions)
                        ), 0
                    ) * 100, 0
                ) AS techstack_score_percent,
                EXISTS (
                    SELECT 1 FROM member_category mc
                    WHERE mc.member_id = :memberId AND mc.category_id = p.domain_id
                ) AS domain_match
            FROM project_embedding pe
            JOIN project p ON pe.project_id = p.project_id
            LEFT JOIN category c ON p.domain_id = c.category_id
            JOIN report_embedding re ON re.report_embedding_id = (
                SELECT re2.report_embedding_id FROM report_embedding re2
                JOIN dev_report dr ON re2.dev_report_id = dr.dev_report_id
                JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
                WHERE gru.member_id = :memberId
                  AND re2.status = 'SUCCESS'
                  AND re2.embedding IS NOT NULL
                ORDER BY re2.created_at DESC LIMIT 1
            )
            WHERE pe.status = 'SUCCESS'
              AND p.project_status = 'RECRUITING'
              AND EXISTS (
                SELECT 1 FROM dev_techstack dt
                JOIN techstack t ON dt.techstack_id = t.techstack_id
                LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
                JOIN project_requirement_member prm ON prm.project_id = p.project_id
                WHERE dt.member_id = :memberId
                  AND prm.current_count < prm.req_mem_num
                  AND (
                    (t.parent_stack IS NULL AND t.techstack_name = prm.req_mem_part)
                    OR (root.parent_stack IS NULL AND root.techstack_name = prm.req_mem_part)
                  )
              )
            """);

        // 동적 필터 조건
        appendFilterConditions(sql, params, projectFields, categories, techstackNames, durationRanges);

        sql.append(" ORDER BY total_score DESC LIMIT :limit");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        return query.getResultList();
    }

    private void appendFilterConditions(
            StringBuilder sql,
            Map<String, Object> params,
            List<ProjectField> projectFields,
            List<CategoryGenre> categories,
            List<TechName> techstackNames,
            List<DurationRange> durationRanges
    ) {
        // 프로젝트 유형 필터
        List<String> fieldStrings = toStringList(projectFields);
        if (fieldStrings != null && !fieldStrings.contains("ALL")) {
            sql.append(" AND p.project_field IN (:projectFields)");
            params.put("projectFields", fieldStrings);
        }

        // 도메인(카테고리) 필터
        List<String> categoryStrings = toStringList(categories);
        if (categoryStrings != null && !categoryStrings.contains("ALL")) {
            sql.append(" AND c.genre IN (:categories)");
            params.put("categories", categoryStrings);
        }

        // 기술스택 필터: 프로젝트가 요구하는 기술스택 중 하나라도 포함
        List<String> techStrings = toStringList(techstackNames);
        if (techStrings != null) {
            sql.append("""
                 AND EXISTS (
                    SELECT 1 FROM project_requirement_techstack prt
                    JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
                    JOIN techstack t ON prt.techstack_id = t.techstack_id
                    WHERE prm.project_id = p.project_id AND t.techstack_name IN (:techstackNames)
                 )
                """);
            params.put("techstackNames", techStrings);
        }

        // 예상 기간 필터
        List<String> durationStrings = toStringList(durationRanges);
        if (durationStrings != null) {
            sql.append(" AND p.duration_range IN (:durationRanges)");
            params.put("durationRanges", durationStrings);
        }
    }

    private <E extends Enum<E>> List<String> toStringList(List<E> enums) {
        if (enums == null || enums.isEmpty()) {
            return null;
        }
        return enums.stream().map(Enum::name).toList();
    }
}
