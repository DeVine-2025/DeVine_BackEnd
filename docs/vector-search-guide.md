# 벡터 검색 구현 가이드

## 개요

이 문서는 기존 쿼리 기반 추천 시스템을 pgvector 기반 벡터 유사도 검색으로 전환하기 위한 구현 가이드입니다.

### 벡터 검색이란

벡터 검색은 텍스트, 이미지 등의 데이터를 고차원 벡터(임베딩)로 변환한 후, 벡터 간 거리/유사도를 계산하여 의미적으로 유사한 항목을 찾는 기술입니다.

### pgvector 코사인 유사도

PostgreSQL의 pgvector 확장은 벡터 연산을 지원하며, 코사인 유사도 연산자 `<=>`를 사용합니다.

```sql
-- 코사인 거리 (낮을수록 유사)
SELECT * FROM items ORDER BY embedding <=> '[0.1, 0.2, ...]' LIMIT 10;

-- 내적 거리
SELECT * FROM items ORDER BY embedding <#> '[0.1, 0.2, ...]' LIMIT 10;

-- L2 거리 (유클리드)
SELECT * FROM items ORDER BY embedding <-> '[0.1, 0.2, ...]' LIMIT 10;
```

> 코사인 유사도는 벡터의 크기보다 방향을 비교하므로 텍스트 임베딩에 적합합니다.

---

## 추천 시스템 개요

### 1. 개발자 추천 (프로젝트 → 개발자)

| 항목 | 설명 |
|------|------|
| **목적** | PM이 프로젝트에 적합한 개발자 상위 10명 추천받음 |
| **로직** | 복합 점수: 리포트 유사도(60%) + 기술스택 매칭(20%) + 도메인 일치도(20%) |
| **필터** | 내 프로젝트 선택 (PM이 소유한 프로젝트 중 선택) |
| **제외 조건** | 개발자의 주 포지션(기술스택 Root)이 해당 프로젝트에서 모집 마감된 경우 |

#### 사용 흐름

```
1. PM이 "개발자 추천" 페이지 진입
2. "내 프로젝트 선택" 필터에서 자신의 프로젝트 목록 표시
3. PM이 특정 프로젝트 선택
4. 선택한 프로젝트 기준으로 상위 10명의 개발자 추천
5. 각 개발자별 점수 정보 표시:
   - 리포트 유사도 (100점 만점)
   - 기술 스택 일치도 (100점 만점)
   - 도메인 적합도 (일치/불일치)
```

#### API 호출 순서

```
1. GET /api/v1/projects/my  → PM의 프로젝트 목록 조회 (필터용)
2. GET /api/v1/members/recommend?projectIds={선택한ID}
   → 선택한 프로젝트 기준 개발자 추천 (상위 10명 고정 반환)
```

### 2. 프로젝트 추천 (개발자 → 프로젝트)

| 항목 | 설명 |
|------|------|
| **목적** | 개발자에게 적합한 프로젝트 상위 10개 추천 |
| **로직** | 복합 점수: 리포트 유사도(60%) + 기술스택 매칭(20%) + 도메인 일치도(20%) |
| **제외 조건** | 개발자의 주 포지션(기술스택 Root)이 해당 프로젝트에서 모집 마감된 경우 |
| **필터** | 프로젝트 유형, 도메인, 기술스택, 예상 기간 (모두 복수 선택 가능, 미선택 시 전체) |

#### 사용 흐름

```
1. 개발자가 "프로젝트 추천" 페이지 진입
2. 필터 선택 (선택 안하면 전체):
   - 프로젝트 유형: WEB, MOBILE, GAME, BLOCKCHAIN, ETC
   - 도메인: HEALTHCARE, FINTECH, ECOMMERCE 등
   - 기술스택: JAVA, REACT, SPRING 등
   - 예상 기간: 1개월 이하 / 1-3개월 / 3-6개월 / 6개월 이상
3. 필터 조건 + 복합 점수 기준으로 상위 프로젝트 추천
4. 각 프로젝트별 점수 정보 표시:
   - 리포트 유사도 (100점 만점)
   - 기술 스택 일치도 (100점 만점)
   - 도메인 적합도 (일치/불일치)
```

#### API 호출

```
GET /api/v1/projects/recommend?projectFields=WEB,MOBILE&categories=FINTECH
→ 필터 적용된 프로젝트 추천 (상위 10개 고정 반환)
```

---

## 선행 작업

### 1. 기존 Techstack 계층 구조 이해

개발자의 주 포지션은 **기존 `DevTechstack` + `Techstack` 계층 구조**를 활용하여 파악합니다. 별도의 필드 추가가 필요 없습니다.

#### Techstack 계층 구조

```
Techstack 엔티티
├── id
├── name (TechName enum)
├── genre (TechGenre enum)
└── parentStack (자기 참조 - 부모 기술스택)
```

#### TechName enum 구조

```java
public enum TechName {
    // Root (최상위 포지션) - parentStack이 NULL
    BACKEND, FRONTEND, INFRA,

    // 상세 기술 - parentStack으로 Root와 연결
    JAVA, PYTHON, GO, C, KOTLIN, PHP,           // → parentStack: BACKEND
    SPRINGBOOT, NODEJS, EXPRESS, NESTJS, ...    // → parentStack: BACKEND
    JAVASCRIPT, TYPESCRIPT, REACT, VUEJS, ...   // → parentStack: FRONTEND
    AWS, FIREBASE, DOCKER, KUBERNETES, ...      // → parentStack: INFRA
}
```

#### 개발자 주 포지션 파악 방법

```sql
-- 개발자의 기술스택 중 Root(parentStack IS NULL)를 찾음
SELECT DISTINCT t.techstack_name
FROM dev_techstack dt
JOIN techstack t ON dt.techstack_id = t.techstack_id
WHERE dt.member_id = :memberId
  AND t.parent_stack IS NULL;

-- 또는 세부 기술스택의 부모를 따라가서 Root 찾기
SELECT DISTINCT parent.techstack_name
FROM dev_techstack dt
JOIN techstack t ON dt.techstack_id = t.techstack_id
JOIN techstack parent ON t.parent_stack = parent.techstack_id
WHERE dt.member_id = :memberId
  AND parent.parent_stack IS NULL;
```

#### TechName ↔ ProjectPart 매핑

| TechName (Root) | ProjectPart | 비고 |
|-----------------|-------------|------|
| `BACKEND` | `BACKEND` | 직접 매핑 |
| `FRONTEND` | `FRONTEND` | 직접 매핑 |
| `INFRA` | `INFRA` | 직접 매핑 |

> **참고**: TechName의 Root(BACKEND, FRONTEND, INFRA)는 ProjectPart와 1:1 매핑됩니다.

---

## 개발자 추천 구현 가이드

> **참고**: 이 섹션은 기존 `GET /api/v1/members/recommend` API를 벡터 검색 기반으로 수정하는 가이드입니다.
> 상세한 기존 코드 분석과 수정 방법은 [기존 코드 수정 가이드](#기존-코드-수정-가이드-개발자-추천) 섹션을 참고하세요.

### 3.1 Repository 메서드 추가

**ReportEmbeddingRepository.java** (추가할 메서드):

```java
package com.umc.devine.domain.report.repository;

import com.umc.devine.domain.report.entity.ReportEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportEmbeddingRepository extends JpaRepository<ReportEmbedding, Long> {

    Optional<ReportEmbedding> findByDevReportId(Long devReportId);

    boolean existsByDevReportId(Long devReportId);

    /**
     * 프로젝트에 적합한 개발자를 복합 점수 기반으로 추천합니다.
     *
     * 복합 점수 (100점 만점):
     * 1. 리포트 유사도: 60점 - 프로젝트 임베딩 ↔ 개발자 리포트 임베딩 코사인 유사도 × 60
     * 2. 기술 스택 매칭: 20점 - (일치 개수 / 프로젝트 요구 개수) × 20
     * 3. 도메인 일치도: 20점 - 기본 10점 + 도메인 일치 시 +10점
     *
     * API 응답용 추가 필드 (프론트엔드 표시용):
     * - similarityScorePercent: 리포트 유사도 (100점 만점)
     * - techstackScorePercent: 기술 스택 일치도 (100점 만점)
     * - domainMatch: 도메인 일치 여부 (true/false)
     *
     * @param projectId 프로젝트 ID
     * @param limit 반환할 최대 개수
     * @return 총점 순으로 정렬된 개발자 정보
     */
    @Query(value = """
        SELECT
            m.member_id,
            m.nickname,
            m.image,
            m.body,
            -- 내부 점수 계산용 (가중치 적용)
            -- 1. 리포트 유사도 점수 (60점 만점)
            (1 - (re.embedding <=> pe.embedding)) * 60 AS similarity_score,
            -- 2. 기술 스택 매칭 점수 (20점 만점)
            COALESCE(
                (SELECT COUNT(DISTINCT dt.techstack_id)::float
                 FROM dev_techstack dt
                 JOIN project_requirement_techstack prt
                   ON dt.techstack_id = prt.techstack_id
                 JOIN project_requirement_member prm
                   ON prt.project_requirement_member_id = prm.project_requirement_member_id
                 WHERE dt.member_id = m.member_id
                   AND prm.project_id = :projectId
                ) / NULLIF(
                    (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                     FROM project_requirement_techstack prt2
                     JOIN project_requirement_member prm2
                       ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                     WHERE prm2.project_id = :projectId
                    ), 0
                ) * 20, 0
            ) AS techstack_score,
            -- 3. 도메인 일치도 점수 (20점 만점, 기본 10점)
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM member_category mc
                    JOIN category c ON mc.category_id = c.category_id
                    WHERE mc.member_id = m.member_id
                      AND c.category_id = p.domain_id
                ) THEN 20
                ELSE 10
            END AS domain_score,
            -- 총점 (100점 만점)
            (1 - (re.embedding <=> pe.embedding)) * 60 +
            COALESCE(
                (SELECT COUNT(DISTINCT dt.techstack_id)::float
                 FROM dev_techstack dt
                 JOIN project_requirement_techstack prt
                   ON dt.techstack_id = prt.techstack_id
                 JOIN project_requirement_member prm
                   ON prt.project_requirement_member_id = prm.project_requirement_member_id
                 WHERE dt.member_id = m.member_id
                   AND prm.project_id = :projectId
                ) / NULLIF(
                    (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                     FROM project_requirement_techstack prt2
                     JOIN project_requirement_member prm2
                       ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                     WHERE prm2.project_id = :projectId
                    ), 0
                ) * 20, 0
            ) +
            CASE
                WHEN EXISTS (
                    SELECT 1 FROM member_category mc
                    JOIN category c ON mc.category_id = c.category_id
                    WHERE mc.member_id = m.member_id
                      AND c.category_id = p.domain_id
                ) THEN 20
                ELSE 10
            END AS total_score,

            -- API 응답용 필드 (100점 만점 환산 및 boolean)
            -- 리포트 유사도 (100점 만점)
            (1 - (re.embedding <=> pe.embedding)) * 100 AS similarity_score_percent,
            -- 기술 스택 일치도 (100점 만점)
            COALESCE(
                (SELECT COUNT(DISTINCT dt.techstack_id)::float
                 FROM dev_techstack dt
                 JOIN project_requirement_techstack prt
                   ON dt.techstack_id = prt.techstack_id
                 JOIN project_requirement_member prm
                   ON prt.project_requirement_member_id = prm.project_requirement_member_id
                 WHERE dt.member_id = m.member_id
                   AND prm.project_id = :projectId
                ) / NULLIF(
                    (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                     FROM project_requirement_techstack prt2
                     JOIN project_requirement_member prm2
                       ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                     WHERE prm2.project_id = :projectId
                    ), 0
                ) * 100, 0
            ) AS techstack_score_percent,
            -- 도메인 일치 여부 (boolean)
            EXISTS (
                SELECT 1 FROM member_category mc
                JOIN category c ON mc.category_id = c.category_id
                WHERE mc.member_id = m.member_id
                  AND c.category_id = p.domain_id
            ) AS domain_match

        FROM report_embedding re
        JOIN dev_report dr ON re.dev_report_id = dr.dev_report_id
        JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
        JOIN member m ON gru.member_id = m.member_id
        JOIN project_embedding pe ON pe.project_id = :projectId
        JOIN project p ON p.project_id = :projectId
        WHERE re.status = 'SUCCESS'
          AND m.main_type = 'DEVELOPER'
          -- 모집 마감 안된 파트의 개발자만
          AND EXISTS (
            SELECT 1 FROM dev_techstack dt
            JOIN techstack t ON dt.techstack_id = t.techstack_id
            LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
            JOIN project_requirement_member prm ON prm.project_id = :projectId
            WHERE dt.member_id = m.member_id
              AND prm.current_count < prm.req_mem_num
              AND (
                (t.parent_stack IS NULL AND t.techstack_name = prm.req_mem_part)
                OR (root.parent_stack IS NULL AND root.techstack_name = prm.req_mem_part)
              )
          )
        ORDER BY total_score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findRecommendedDevelopersForProject(
        @Param("projectId") Long projectId,
        @Param("limit") int limit
    );

    /**
     * 개발자의 리포트 임베딩을 조회합니다.
     *
     * @param memberId 개발자 ID
     * @return 해당 개발자의 리포트 임베딩 (성공 상태인 것만)
     */
    @Query(value = """
        SELECT re.* FROM report_embedding re
        JOIN dev_report dr ON re.dev_report_id = dr.dev_report_id
        JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
        WHERE gru.member_id = :memberId
          AND re.status = 'SUCCESS'
        ORDER BY re.created_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<ReportEmbedding> findByMemberId(@Param("memberId") Long memberId);

    /**
     * 개발자의 주 포지션(기술스택 Root)을 조회합니다.
     *
     * @param memberId 개발자 ID
     * @return 주 포지션 목록 (BACKEND, FRONTEND 등)
     */
    @Query(value = """
        SELECT DISTINCT
          CASE
            WHEN t.parent_stack IS NULL THEN t.techstack_name
            ELSE root.techstack_name
          END as root_position
        FROM dev_techstack dt
        JOIN techstack t ON dt.techstack_id = t.techstack_id
        LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
        WHERE dt.member_id = :memberId
          AND (t.parent_stack IS NULL OR root.parent_stack IS NULL)
        """, nativeQuery = true)
    List<String> findMemberRootPositions(@Param("memberId") Long memberId);
}
```

### 3.2 Controller (기존 API 사용)

> **참고**: 새 Controller를 만들지 않고, 기존 `MemberController`의 API를 그대로 사용합니다.

**기존 엔드포인트:**

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/members/recommend` | 개발자 추천 (상위 10명 고정) |

**기존 Request 파라미터 (`RecommendDeveloperDTO` 수정 필요):**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `projectIds` | Long[] | **내 프로젝트 선택 필터** (필수) |

> **참고**: 기존 DTO의 `page`, `size`, `category`, `techGenre`, `techstackName` 파라미터는 제거합니다. 고정 10개 반환이므로 페이지네이션 불필요.

**호출 예시:**
```
GET /api/v1/members/recommend?projectIds=1
```

### 3.3 Response DTO (기존 수정)

> **참고**: 새 DTO를 만들지 않고, 기존 `MemberResDTO.DeveloperDTO`에 점수 필드를 추가합니다.

**MemberResDTO.java 수정:**

```java
// 기존 DeveloperDTO에 점수 필드 추가
@Builder
public record DeveloperDTO(
    Long memberId,
    String nickname,
    String image,
    String body,
    List<TechstackDTO> techstacks,

    // ===== 추가할 필드 (복합 점수 정보) =====
    Double totalScore,              // 총점 (100점 만점)
    Double similarityScorePercent,  // 리포트 유사도 (100점 만점)
    Double techstackScorePercent,   // 기술 스택 일치도 (100점 만점)
    Boolean domainMatch             // 도메인 일치 여부
) {}

// 개발자 추천 응답 DTO (신규)
@Builder
public record RecommendedDevelopersRes(
    @Schema(description = "기준 프로젝트 ID")
    Long projectId,

    @Schema(description = "추천 개발자 목록 (상위 10명)")
    List<DeveloperDTO> developers,

    @Schema(description = "반환된 개발자 수")
    Integer count
) {}
```

**API 응답 예시:**
```json
{
  "isSuccess": true,
  "code": "200",
  "result": {
    "projectId": 1,
    "developers": [
      {
        "memberId": 123,
        "nickname": "개발자A",
        "totalScore": 83.0,
        "similarityScorePercent": 80.0,
        "techstackScorePercent": 75.0,
        "domainMatch": true
      }
    ],
    "count": 10
  }
}
```

---

## 프로젝트 추천 구현 가이드

> **참고**: 이 섹션은 기존 `GET /api/v1/projects/recommend` API를 벡터 검색 + 복합 점수 기반으로 수정하는 가이드입니다.

### 4.1 Repository 메서드 추가

**ProjectEmbeddingRepository.java** (전체 인터페이스):

```java
package com.umc.devine.domain.project.repository;

import com.umc.devine.domain.project.entity.ProjectEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectEmbeddingRepository extends JpaRepository<ProjectEmbedding, Long> {

    Optional<ProjectEmbedding> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);
```

**추가할 메서드**:

```java
/**
 * 개발자에게 적합한 프로젝트를 복합 점수 기반으로 추천합니다.
 *
 * 복합 점수 (100점 만점):
 * 1. 리포트 유사도: 60점 - 개발자 리포트 임베딩 ↔ 프로젝트 임베딩 코사인 유사도 × 60
 * 2. 기술 스택 매칭: 20점 - (프로젝트 요구 기술스택 중 개발자 보유 개수 / 프로젝트 요구 개수) × 20
 * 3. 도메인 일치도: 20점 - 기본 10점 + 프로젝트 도메인이 개발자 관심 도메인에 포함 시 +10점
 *
 * @param memberId 개발자 ID
 * @param limit 반환할 최대 개수
 * @return 총점 순으로 정렬된 프로젝트 정보
 */
@Query(value = """
    SELECT
        p.project_id,
        p.project_name AS title,
        p.project_field,
        p.duration_range,
        c.genre as category,

        -- 1. 리포트 유사도 점수 (60점 만점)
        (1 - (re.embedding <=> pe.embedding)) * 60 AS similarity_score,

        -- 2. 기술 스택 매칭 점수 (20점 만점)
        COALESCE(
            (SELECT COUNT(DISTINCT dt.techstack_id)::float
             FROM dev_techstack dt
             JOIN project_requirement_techstack prt
               ON dt.techstack_id = prt.techstack_id
             JOIN project_requirement_member prm
               ON prt.project_requirement_member_id = prm.project_requirement_member_id
             WHERE dt.member_id = :memberId
               AND prm.project_id = p.project_id
            ) / NULLIF(
                (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                 FROM project_requirement_techstack prt2
                 JOIN project_requirement_member prm2
                   ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                 WHERE prm2.project_id = p.project_id
                ), 0
            ) * 20, 0
        ) AS techstack_score,

        -- 3. 도메인 일치도 점수 (20점 만점, 기본 10점)
        CASE
            WHEN EXISTS (
                SELECT 1 FROM member_category mc
                WHERE mc.member_id = :memberId
                  AND mc.category_id = p.domain_id
            ) THEN 20
            ELSE 10
        END AS domain_score,

        -- 총점 (100점 만점)
        (1 - (re.embedding <=> pe.embedding)) * 60 +
        COALESCE(
            (SELECT COUNT(DISTINCT dt.techstack_id)::float
             FROM dev_techstack dt
             JOIN project_requirement_techstack prt
               ON dt.techstack_id = prt.techstack_id
             JOIN project_requirement_member prm
               ON prt.project_requirement_member_id = prm.project_requirement_member_id
             WHERE dt.member_id = :memberId
               AND prm.project_id = p.project_id
            ) / NULLIF(
                (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                 FROM project_requirement_techstack prt2
                 JOIN project_requirement_member prm2
                   ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                 WHERE prm2.project_id = p.project_id
                ), 0
            ) * 20, 0
        ) +
        CASE
            WHEN EXISTS (
                SELECT 1 FROM member_category mc
                WHERE mc.member_id = :memberId
                  AND mc.category_id = p.domain_id
            ) THEN 20
            ELSE 10
        END AS total_score,

        -- API 응답용 필드 (100점 만점 환산)
        (1 - (re.embedding <=> pe.embedding)) * 100 AS similarity_score_percent,
        COALESCE(
            (SELECT COUNT(DISTINCT dt.techstack_id)::float
             FROM dev_techstack dt
             JOIN project_requirement_techstack prt
               ON dt.techstack_id = prt.techstack_id
             JOIN project_requirement_member prm
               ON prt.project_requirement_member_id = prm.project_requirement_member_id
             WHERE dt.member_id = :memberId
               AND prm.project_id = p.project_id
            ) / NULLIF(
                (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                 FROM project_requirement_techstack prt2
                 JOIN project_requirement_member prm2
                   ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                 WHERE prm2.project_id = p.project_id
                ), 0
            ) * 100, 0
        ) AS techstack_score_percent,
        EXISTS (
            SELECT 1 FROM member_category mc
            WHERE mc.member_id = :memberId
              AND mc.category_id = p.domain_id
        ) AS domain_match

    FROM project_embedding pe
    JOIN project p ON pe.project_id = p.project_id
    LEFT JOIN category c ON p.domain_id = c.category_id
    JOIN report_embedding re ON re.dev_report_id = (
        SELECT dr.dev_report_id FROM dev_report dr
        JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
        WHERE gru.member_id = :memberId
        ORDER BY dr.created_at DESC LIMIT 1
    )
    WHERE pe.status = 'SUCCESS'
      AND p.project_status = 'RECRUITING'
      -- 개발자의 기술스택 Root가 프로젝트에서 아직 모집 중인지 확인
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
    ORDER BY total_score DESC
    LIMIT :limit
    """, nativeQuery = true)
List<Object[]> findRecommendedProjectsForMember(
    @Param("memberId") Long memberId,
    @Param("limit") int limit
);

/**
 * 필터 조건이 포함된 프로젝트 추천 쿼리
 *
 * 참고: Native Query에서 동적 필터를 처리하는 방법
 * - NULL 파라미터는 "전체" 선택으로 처리
 * - 복수 선택 필터는 IN 절로 처리
 */
@Query(value = """
    SELECT
        p.project_id,
        p.project_name AS title,
        p.project_field,
        p.duration_range,
        c.genre as category,
        (1 - (re.embedding <=> pe.embedding)) * 60 AS similarity_score,
        COALESCE(
            (SELECT COUNT(DISTINCT dt.techstack_id)::float
             FROM dev_techstack dt
             JOIN project_requirement_techstack prt ON dt.techstack_id = prt.techstack_id
             JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
             WHERE dt.member_id = :memberId AND prm.project_id = p.project_id
            ) / NULLIF(
                (SELECT COUNT(DISTINCT prt2.techstack_id)::float
                 FROM project_requirement_techstack prt2
                 JOIN project_requirement_member prm2 ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
                 WHERE prm2.project_id = p.project_id
                ), 0
            ) * 20, 0
        ) AS techstack_score,
        CASE WHEN EXISTS (SELECT 1 FROM member_category mc WHERE mc.member_id = :memberId AND mc.category_id = p.domain_id) THEN 20 ELSE 10 END AS domain_score,
        -- total_score (합산)
        (1 - (re.embedding <=> pe.embedding)) * 60 +
        COALESCE((SELECT COUNT(DISTINCT dt.techstack_id)::float FROM dev_techstack dt
             JOIN project_requirement_techstack prt ON dt.techstack_id = prt.techstack_id
             JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
             WHERE dt.member_id = :memberId AND prm.project_id = p.project_id
            ) / NULLIF((SELECT COUNT(DISTINCT prt2.techstack_id)::float FROM project_requirement_techstack prt2
             JOIN project_requirement_member prm2 ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
             WHERE prm2.project_id = p.project_id), 0) * 20, 0) +
        CASE WHEN EXISTS (SELECT 1 FROM member_category mc WHERE mc.member_id = :memberId AND mc.category_id = p.domain_id) THEN 20 ELSE 10 END AS total_score,
        (1 - (re.embedding <=> pe.embedding)) * 100 AS similarity_score_percent,
        COALESCE((SELECT COUNT(DISTINCT dt.techstack_id)::float FROM dev_techstack dt
             JOIN project_requirement_techstack prt ON dt.techstack_id = prt.techstack_id
             JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
             WHERE dt.member_id = :memberId AND prm.project_id = p.project_id
            ) / NULLIF((SELECT COUNT(DISTINCT prt2.techstack_id)::float FROM project_requirement_techstack prt2
             JOIN project_requirement_member prm2 ON prt2.project_requirement_member_id = prm2.project_requirement_member_id
             WHERE prm2.project_id = p.project_id), 0) * 100, 0) AS techstack_score_percent,
        EXISTS (SELECT 1 FROM member_category mc WHERE mc.member_id = :memberId AND mc.category_id = p.domain_id) AS domain_match
    FROM project_embedding pe
    JOIN project p ON pe.project_id = p.project_id
    LEFT JOIN category c ON p.domain_id = c.category_id
    JOIN report_embedding re ON re.dev_report_id = (
        SELECT dr.dev_report_id FROM dev_report dr
        JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
        WHERE gru.member_id = :memberId ORDER BY dr.created_at DESC LIMIT 1
    )
    WHERE pe.status = 'SUCCESS'
      AND p.project_status = 'RECRUITING'
      -- 필터 조건: NULL이면 전체, 값이 있으면 해당 값만
      AND (:projectFields IS NULL OR p.project_field IN :projectFields)
      AND (:categories IS NULL OR c.genre IN :categories)
      AND (:durationRanges IS NULL OR p.duration_range IN :durationRanges)
      -- 기술스택 필터: 프로젝트가 요구하는 기술스택 중 하나라도 포함
      AND (:techstackNames IS NULL OR EXISTS (
          SELECT 1 FROM project_requirement_techstack prt
          JOIN project_requirement_member prm ON prt.project_requirement_member_id = prm.project_requirement_member_id
          JOIN techstack t ON prt.techstack_id = t.techstack_id
          WHERE prm.project_id = p.project_id AND t.techstack_name IN :techstackNames
      ))
      -- 개발자 포지션 필터
      AND EXISTS (
        SELECT 1 FROM dev_techstack dt
        JOIN techstack t ON dt.techstack_id = t.techstack_id
        LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
        JOIN project_requirement_member prm ON prm.project_id = p.project_id
        WHERE dt.member_id = :memberId
          AND prm.current_count < prm.req_mem_num
          AND ((t.parent_stack IS NULL AND t.techstack_name = prm.req_mem_part)
               OR (root.parent_stack IS NULL AND root.techstack_name = prm.req_mem_part))
      )
    ORDER BY total_score DESC
    LIMIT :limit
    """, nativeQuery = true)
List<Object[]> findRecommendedProjectsWithFilters(
    @Param("memberId") Long memberId,
    @Param("projectFields") List<String> projectFields,
    @Param("categories") List<String> categories,
    @Param("techstackNames") List<String> techstackNames,
    @Param("durationRanges") List<String> durationRanges,
    @Param("limit") int limit
);
}
```

### 4.2 기존 구조 분석

기존 프로젝트 추천 API가 **Project 도메인**에 이미 존재합니다.

#### 기존 엔드포인트

| 엔드포인트 | 설명 | 수정 사항 |
|-----------|------|----------|
| `GET /api/v1/projects/recommend` | 프로젝트 추천 | 상위 10개 고정 반환으로 변경 |

#### 기존 파일 위치

```
com.umc.devine.domain.project
├── controller
│   └── ProjectController.java              # 엔드포인트 정의
├── dto
│   ├── ProjectReqDTO.java                  # RecommendProjectsReq (페이지네이션 제거)
│   └── ProjectResDTO.java                  # RecommendedProjectSummary (점수 필드 수정 필요)
├── service/query
│   └── ProjectQueryServiceImpl.java        # getRecommendedProjects() - 수정 대상
```

#### Request DTO 수정 (`RecommendProjectsReq`)

```java
// 기존 RecommendProjectsPageReq → RecommendProjectsReq로 변경
// page, size 파라미터 제거 (고정 10개 반환)
public record RecommendProjectsReq(
    List<ProjectField> projectFields,     // ✅ 프로젝트 유형 (복수 선택, 미선택 시 전체)
    List<CategoryGenre> categories,       // ✅ 도메인 (복수 선택, 미선택 시 전체)
    List<TechName> techstackNames,        // ✅ 기술스택 (복수 선택, 미선택 시 전체)
    List<DurationRange> durationRanges    // ✅ 예상 기간 (복수 선택, 미선택 시 전체)
) {}
```

> **참고**: `positions` 필터는 개발자 주 포지션으로 자동 필터링되므로 제거

#### 기존 Service 로직 (TODO 상태)

```java
// ProjectQueryServiceImpl.java - 수정 필요!
@Override
public ProjectResDTO.RecommendedProjectsRes getRecommendedProjects(
        Member member,
        ProjectReqDTO.RecommendProjectsReq request
) {
    // TODO: 추천 알고리즘 기반 정렬 추가  ← 벡터 검색으로 수정!
    // 기존: 단순 필터링만 수행 중
}
```

---

### 4.3 Response DTO 수정

**ProjectResDTO.java** - `RecommendedProjectSummary` 및 `RecommendedProjectsRes` 수정:

```java
// 기존 점수 필드 (0~5 스케일) → 새로운 점수 필드 (100점 만점)로 변경
@Builder
public record RecommendedProjectSummary(
    @Schema(description = "프로젝트 ID", example = "1")
    Long projectId,

    @Schema(description = "프로젝트 제목", example = "AI 기반 추천 시스템")
    String title,

    @Schema(description = "프로젝트 분야")
    ProjectField projectField,

    @Schema(description = "프로젝트 분야 이름", example = "웹")
    String projectFieldName,

    @Schema(description = "카테고리", example = "ECOMMERCE")
    CategoryGenre category,

    @Schema(description = "카테고리 이름", example = "이커머스")
    String categoryName,

    @Schema(description = "진행 방식", example = "ONLINE")
    ProjectMode mode,

    @Schema(description = "진행 방식 이름", example = "온라인")
    String modeName,

    @Schema(description = "진행 기간", example = "ONE_TO_THREE")
    DurationRange durationRange,

    @Schema(description = "진행 기간 이름", example = "1~3개월")
    String durationRangeName,

    @Schema(description = "진행 장소", example = "서울 강남구")
    String location,

    @Schema(description = "모집 마감일", example = "2026-01-25")
    LocalDate recruitmentDeadline,

    @Schema(description = "모집 마감까지 남은 일수", example = "5")
    Long daysUntilDeadline,

    @Schema(description = "프로젝트 상태")
    ProjectStatus status,

    @Schema(description = "썸네일 이미지 URL")
    String thumbnailUrl,

    @Schema(description = "모집 포지션 목록")
    List<PositionSummary> positions,

    @Schema(description = "생성자 이름", example = "김개발")
    String creatorName,

    // ===== 점수 필드 (100점 만점) =====
    @Schema(description = "총 추천 점수 (100점 만점)", example = "85.0")
    Double totalScore,

    @Schema(description = "리포트 유사도 (100점 만점)", example = "90.0")
    Double similarityScorePercent,

    @Schema(description = "기술 스택 일치도 (100점 만점)", example = "75.0")
    Double techstackScorePercent,

    @Schema(description = "도메인 일치 여부", example = "true")
    Boolean domainMatch
) {}

// 프로젝트 추천 응답 DTO 수정 (PagedResponse → List)
@Builder
public record RecommendedProjectsRes(
    @Schema(description = "추천 프로젝트 목록 (상위 10개)")
    List<RecommendedProjectSummary> projects,

    @Schema(description = "반환된 프로젝트 수")
    Integer count
) {}
```

### 4.4 Controller (기존 API 사용)

> **참고**: 새 Controller를 만들지 않고, 기존 `ProjectController`의 API를 그대로 사용합니다.

**기존 엔드포인트:**

| 메서드 | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/projects/recommend` | 프로젝트 추천 (상위 10개 고정 + 필터) |

**필터 파라미터:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `projectFields` | List | 프로젝트 유형 (복수 선택, 미선택 시 전체) |
| `categories` | List | 도메인 (복수 선택, 미선택 시 전체) |
| `techstackNames` | List | 기술스택 (복수 선택, 미선택 시 전체) |
| `durationRanges` | List | 예상 기간 (복수 선택, 미선택 시 전체) |

> **참고**: 기존 DTO의 `page`, `size` 파라미터는 제거합니다. 고정 10개 반환이므로 페이지네이션 불필요.

**호출 예시:**
```
GET /api/v1/projects/recommend?projectFields=WEB,MOBILE&categories=FINTECH&durationRanges=ONE_TO_THREE,THREE_TO_SIX
```

**API 응답 예시:**
```json
{
  "isSuccess": true,
  "code": "200",
  "result": {
    "projects": [
      {
        "projectId": 1,
        "title": "AI 협업 플랫폼",
        "projectField": "WEB",
        "category": "FINTECH",
        "durationRange": "ONE_TO_THREE",
        "totalScore": 85.0,
        "similarityScorePercent": 90.0,
        "techstackScorePercent": 75.0,
        "domainMatch": true
      }
    ],
    "count": 10
  }
}
```

### 4.5 Service 로직 수정

**ProjectQueryServiceImpl.java** - `getRecommendedProjects()` 수정:

```java
private static final int RECOMMEND_LIMIT = 10;  // 고정 10개

@Override
public ProjectResDTO.RecommendedProjectsRes getRecommendedProjects(
        Member member,
        ProjectReqDTO.RecommendProjectsReq request
) {
    // 1. 개발자가 아니거나 리포트가 없으면 기본 추천 (기존 로직)
    if (member.getMainType() != MemberMainType.DEVELOPER) {
        return getDefaultRecommendations();
    }

    Optional<ReportEmbedding> reportEmbedding = reportEmbeddingRepository.findByMemberId(member.getId());
    if (reportEmbedding.isEmpty() || reportEmbedding.get().getEmbedding() == null) {
        return getDefaultRecommendations();
    }

    // 2. 복합 점수 기반 프로젝트 추천 쿼리 실행 (상위 10개 고정)
    List<Object[]> results = projectEmbeddingRepository
        .findRecommendedProjectsWithFilters(
            member.getId(),
            request.projectFields(),
            request.categories(),
            request.techstackNames(),
            request.durationRanges(),
            RECOMMEND_LIMIT
        );

    if (results.isEmpty()) {
        return ProjectResDTO.RecommendedProjectsRes.builder()
            .projects(List.of())
            .count(0)
            .build();
    }

    // 3. N+1 방지: projectIds 추출 후 프로젝트 한 번에 조회
    List<Long> projectIds = results.stream()
        .map(row -> ((Number) row[0]).longValue())
        .toList();

    // IN 쿼리로 프로젝트 한 번에 조회
    List<Project> projects = projectRepository.findAllByIdIn(projectIds);

    // projectId별로 Map 생성
    Map<Long, Project> projectMap = projects.stream()
        .collect(Collectors.toMap(Project::getId, p -> p));

    // 4. 결과 변환 (추가 DB 조회 없음)
    List<ProjectResDTO.RecommendedProjectSummary> summaries = results.stream()
        .map(row -> convertToRecommendedProjectSummary(row, projectMap))
        .toList();

    return ProjectResDTO.RecommendedProjectsRes.builder()
        .projects(summaries)
        .count(summaries.size())
        .build();
}

/**
 * 기본 추천 (임베딩 없는 경우): 최신 모집 중인 프로젝트 10개 반환
 */
private ProjectResDTO.RecommendedProjectsRes getDefaultRecommendations() {
    // 최신 모집 중인 프로젝트 10개 조회
    List<Project> projects = projectRepository.findTop10ByStatusOrderByCreatedAtDesc(
        ProjectStatus.RECRUITING
    );

    List<ProjectResDTO.RecommendedProjectSummary> summaries = projects.stream()
        .map(project -> convertProjectToSummary(project, null, null, null, null))
        .toList();

    return ProjectResDTO.RecommendedProjectsRes.builder()
        .projects(summaries)
        .count(summaries.size())
        .build();
}

/**
 * 쿼리 결과를 RecommendedProjectSummary로 변환 (N+1 방지: Map 사용)
 *
 * 쿼리 결과 순서:
 * [0] project_id, [1] title, [2] project_field, [3] duration_range, [4] category,
 * [5] similarity_score, [6] techstack_score, [7] domain_score, [8] total_score,
 * [9] similarity_score_percent, [10] techstack_score_percent, [11] domain_match
 */
private ProjectResDTO.RecommendedProjectSummary convertToRecommendedProjectSummary(
        Object[] row,
        Map<Long, Project> projectMap
) {
    Long projectId = ((Number) row[0]).longValue();
    Double totalScore = ((Number) row[8]).doubleValue();
    Double similarityScorePercent = ((Number) row[9]).doubleValue();
    Double techstackScorePercent = ((Number) row[10]).doubleValue();
    Boolean domainMatch = (Boolean) row[11];

    // Map에서 조회 (추가 DB 호출 없음)
    Project project = projectMap.get(projectId);
    if (project == null) {
        throw new ProjectException(PROJECT_NOT_FOUND);
    }

    return convertProjectToSummary(project, totalScore, similarityScorePercent,
                                    techstackScorePercent, domainMatch);
}

/**
 * Project 엔티티를 RecommendedProjectSummary로 변환
 */
private ProjectResDTO.RecommendedProjectSummary convertProjectToSummary(
        Project project,
        Double totalScore,
        Double similarityScorePercent,
        Double techstackScorePercent,
        Boolean domainMatch
) {
    // 썸네일 이미지 URL (첫 번째 이미지)
    String thumbnailUrl = project.getImages().isEmpty()
        ? null
        : project.getImages().get(0).getImageUrl();

    // 모집 마감까지 남은 일수 계산
    long daysUntilDeadline = ChronoUnit.DAYS.between(
        LocalDate.now(),
        project.getRecruitmentDeadline()
    );

    // 모집 포지션 목록 변환
    List<ProjectResDTO.PositionSummary> positions = project.getRequirements().stream()
        .map(req -> ProjectResDTO.PositionSummary.builder()
            .position(req.getPart())
            .positionName(req.getPart().getDisplayName())
            .count(req.getRequirementNum())
            .currentCount(req.getCurrentCount())
            .techStacks(req.getTechstacks().stream()
                .map(prt -> ProjectResDTO.TechStackInfo.builder()
                    .techStack(prt.getTechstack().getName())
                    .build())
                .toList())
            .build())
        .toList();

    return ProjectResDTO.RecommendedProjectSummary.builder()
        .projectId(project.getId())
        .title(project.getTitle())
        .projectField(project.getProjectField())
        .projectFieldName(project.getProjectField().getName())
        .category(project.getCategory().getGenre())
        .categoryName(project.getCategory().getGenre().getName())
        .mode(project.getMode())
        .modeName(project.getMode().getDisplayName())
        .durationRange(project.getDurationRange())
        .durationRangeName(project.getDurationRange().getDisplayName())
        .location(project.getLocation())
        .recruitmentDeadline(project.getRecruitmentDeadline())
        .daysUntilDeadline(daysUntilDeadline)
        .status(project.getStatus())
        .thumbnailUrl(thumbnailUrl)
        .positions(positions)
        .creatorName(project.getMember().getNickname())
        .totalScore(totalScore != null ? Math.round(totalScore * 10) / 10.0 : null)
        .similarityScorePercent(similarityScorePercent != null
            ? Math.round(similarityScorePercent * 10) / 10.0 : null)
        .techstackScorePercent(techstackScorePercent != null
            ? Math.round(techstackScorePercent * 10) / 10.0 : null)
        .domainMatch(domainMatch)
        .build();
}
```

#### 필요한 import

```java
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;
```

#### ProjectRepository 추가 메서드

```java
// ProjectRepository.java에 추가

// getDefaultRecommendations()에서 사용
List<Project> findTop10ByStatusOrderByCreatedAtDesc(ProjectStatus status);

// N+1 방지용 IN 쿼리 (getRecommendedProjects에서 사용)
@Query("SELECT p FROM Project p LEFT JOIN FETCH p.images LEFT JOIN FETCH p.requirements r LEFT JOIN FETCH r.techstacks WHERE p.id IN :ids")
List<Project> findAllByIdIn(@Param("ids") List<Long> ids);
```

---

## SQL 쿼리 예시

> **참고**: 전체 SQL 쿼리는 [개발자 추천 Repository 메서드](#31-repository-메서드-추가) 및 [프로젝트 추천 Repository 메서드](#41-repository-메서드-추가) 섹션을 참고하세요.

### 복합 점수 체계 (100점 만점)

| 점수 항목 | 배점 | 계산 방법 |
|----------|------|----------|
| 리포트 유사도 | 60점 | `(1 - 코사인거리) × 60` |
| 기술 스택 매칭 | 20점 | `(일치 개수 / 프로젝트 요구 개수) × 20` |
| 도메인 일치도 | 20점 | 기본 10점 + 도메인 일치 시 +10점 |

**점수 계산 예시:**
- 리포트 유사도 0.8 → 0.8 × 60 = 48점 (가중치 적용) / 80점 (100점 환산)
- 기술스택 3/4 일치 → 0.75 × 20 = 15점 (가중치 적용) / 75점 (100점 환산)
- 도메인 일치 → 20점 (가중치 적용) / true (boolean)
- **총점: 83점**

**API 응답 예시:**
```json
{
  "isSuccess": true,
  "code": "200",
  "message": "OK",
  "result": {
    "projectId": 1,
    "developers": [
      {
        "memberId": 123,
        "nickname": "개발자A",
        "image": "https://...",
        "mainPosition": "BACKEND",
        "body": "3년차 백엔드 개발자입니다.",
        "totalScore": 83.0,
        "similarityScorePercent": 80.0,
        "techstackScorePercent": 75.0,
        "domainMatch": true
      },
      {
        "memberId": 456,
        "nickname": "개발자B",
        "image": "https://...",
        "mainPosition": "FRONTEND",
        "body": "React 전문 개발자",
        "totalScore": 72.5,
        "similarityScorePercent": 65.0,
        "techstackScorePercent": 50.0,
        "domainMatch": true
      }
    ],
    "count": 2
  }
}
```

### 프로젝트 추천 쿼리 (필터 포함)

```sql
-- 개발자 ID = 456에게 적합한 프로젝트 상위 10개 찾기
-- 조건: 필터 + 개발자의 기술스택 Root가 아직 모집 중인 프로젝트

SELECT
    p.project_id,
    p.project_name AS title,
    p.project_field,
    p.duration_range,
    pe.embedding <=> re.embedding AS distance
FROM project_embedding pe
JOIN project p ON pe.project_id = p.project_id
LEFT JOIN category c ON p.domain_id = c.category_id
JOIN report_embedding re ON re.dev_report_id = (
    SELECT dr.dev_report_id FROM dev_report dr
    JOIN git_repo_url gru ON dr.git_repo_id = gru.git_repo_id
    WHERE gru.member_id = 456
    ORDER BY dr.created_at DESC LIMIT 1
)
WHERE pe.status = 'SUCCESS'
  AND p.project_status = 'RECRUITING'
  -- 필터 조건들 (예시)
  AND p.project_field = 'WEB'
  AND c.genre = 'DEVELOPMENT'
  AND p.duration_range = 'ONE_TO_THREE'
  -- 개발자의 기술스택 Root가 아직 모집 중인 프로젝트만
  AND EXISTS (
    SELECT 1 FROM dev_techstack dt
    JOIN techstack t ON dt.techstack_id = t.techstack_id
    LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
    JOIN project_requirement_member prm ON prm.project_id = p.project_id
    WHERE dt.member_id = 456
      AND prm.current_count < prm.req_mem_num
      AND (
        (t.parent_stack IS NULL AND t.techstack_name = prm.req_mem_part)
        OR (root.parent_stack IS NULL AND root.techstack_name = prm.req_mem_part)
      )
  )
ORDER BY pe.embedding <=> re.embedding
LIMIT 10;
```

### 개발자 주 포지션 조회 쿼리

```sql
-- 개발자 ID = 456의 주 포지션(기술스택 Root) 조회
SELECT DISTINCT
  CASE
    WHEN t.parent_stack IS NULL THEN t.techstack_name
    ELSE root.techstack_name
  END as root_position
FROM dev_techstack dt
JOIN techstack t ON dt.techstack_id = t.techstack_id
LEFT JOIN techstack root ON t.parent_stack = root.techstack_id
WHERE dt.member_id = 456
  AND (t.parent_stack IS NULL OR root.parent_stack IS NULL);
```

---

## 성능 최적화

### 쿼리 최적화 팁

1. **필요한 컬럼만 조회**
```java
// 전체 엔티티 대신 필요한 필드만 프로젝션
@Query(value = """
    SELECT m.member_id, m.nickname, m.preferred_part
    FROM report_embedding re
    JOIN dev_report dr ON re.dev_report_id = dr.dev_report_id
    JOIN member m ON dr.member_id = m.member_id
    WHERE ...
    """, nativeQuery = true)
List<Object[]> findSimilarDeveloperIds(...);
```

2. **LIMIT 활용**
```sql
-- 항상 LIMIT을 사용하여 결과 수 제한
ORDER BY re.embedding <=> :queryVector
LIMIT 10;
```

3. **EXISTS vs IN 성능**
```sql
-- EXISTS가 일반적으로 더 빠름 (일치하는 첫 번째 행에서 중단)
AND EXISTS (
    SELECT 1 FROM project_requirement_member prm
    WHERE prm.project_id = p.project_id
      AND prm.req_mem_part = m.preferred_part
)

-- IN은 서브쿼리 결과가 작을 때 적합
AND m.preferred_part IN (
    SELECT prm.req_mem_part FROM project_requirement_member prm
    WHERE prm.project_id = :projectId
)
```

4. **벡터 캐스팅**
```sql
-- JPA에서 float[] 전달 시 명시적 캐스팅 필요
ORDER BY pe.embedding <=> cast(:queryVector as vector)
```

---

## 수정 대상 파일

```
com.umc.devine
├── domain.member                              # 개발자 추천 (기존 수정)
│   ├── dto
│   │   ├── MemberReqDTO.java                  # RecommendDeveloperDTO 수정 (page, size 제거)
│   │   └── MemberResDTO.java                  # DeveloperDTO 점수 필드 추가, RecommendedDevelopersRes 신규
│   └── service/query
│       └── MemberQueryServiceImpl.java        # getRecommendedDevelopers() 신규 (고정 10명)
│
├── domain.project                             # 프로젝트 추천 (기존 수정)
│   ├── dto
│   │   ├── ProjectReqDTO.java                 # RecommendProjectsReq 수정 (page, size 제거)
│   │   └── ProjectResDTO.java                 # RecommendedProjectSummary 점수 필드 수정
│   └── service/query
│       └── ProjectQueryServiceImpl.java       # getRecommendedProjects() 수정 (고정 10개)
│
├── domain.report/repository
│   └── ReportEmbeddingRepository.java         # 개발자 추천용 복합 점수 쿼리 추가
│
└── domain.project/repository
    └── ProjectEmbeddingRepository.java        # 프로젝트 추천용 복합 점수 쿼리 추가
```

> **참고**: 새 도메인 패키지 생성 불필요 - 기존 member, project 도메인 수정

---

## 기존 코드 수정 가이드 (개발자 추천)

### 현재 구조 분석

기존 개발자 추천 API가 **Member 도메인**에 이미 존재합니다.

#### 기존 엔드포인트

| 엔드포인트 | 설명 | 수정 사항 |
|-----------|------|----------|
| `GET /api/v1/members/recommend` | 개발자 추천 | 상위 10명 고정 반환으로 변경 |

#### 기존 파일 위치

```
com.umc.devine.domain.member
├── controller
│   └── MemberController.java          # 엔드포인트 정의
├── dto
│   └── MemberReqDTO.java              # RecommendDeveloperDTO (수정 필요)
├── service/query
│   └── MemberQueryServiceImpl.java    # getRecommendedDevelopers() - 신규 구현
```

#### 기존 Request DTO (수정 필요)

```java
// 기존 (페이지네이션 + 필터 포함)
public record RecommendDeveloperDTO(
    Long[] projectIds,        // ✅ 유지 (필수)
    CategoryGenre category,   // ❌ 제거
    TechGenre techGenre,      // ❌ 제거
    TechName techstackName,   // ❌ 제거
    Integer page,             // ❌ 제거
    Integer size              // ❌ 제거
) {}

// 변경 후 (projectIds만 유지)
public record RecommendDeveloperDTO(
    Long[] projectIds         // ✅ 내 프로젝트 선택 필터 (필수)
) {}
```

#### 기존 Service 로직 (TODO 상태 → 신규 구현)

```java
// MemberQueryServiceImpl.java - 기존: findAllDevelopers()
// TODO: projectIds를 활용한 추천 로직 구현  ← 벡터 검색으로 변경!
// 기존: 단순 필터링만 수행 중
// 변경: getRecommendedDevelopers()로 신규 구현 (고정 10명 반환)
```

---

### 수정 방법

#### Step 1: Response DTO 수정 (`MemberResDTO.DeveloperDTO`)

점수 정보 필드를 추가합니다.

**파일**: `MemberResDTO.java`

```java
@Builder
public record DeveloperDTO(
    Long memberId,
    String nickname,
    String image,
    String body,
    List<TechstackDTO> techstacks,
    // 추가 필드 (복합 점수 정보)
    Double totalScore,              // 총점 (100점 만점)
    Double similarityScorePercent,  // 리포트 유사도 (100점 만점)
    Double techstackScorePercent,   // 기술 스택 일치도 (100점 만점)
    Boolean domainMatch             // 도메인 일치 여부
) {}
```

#### Step 2: Service 로직 수정 (`MemberQueryServiceImpl`)

**파일**: `MemberQueryServiceImpl.java`

```java
private static final int RECOMMEND_LIMIT = 10;  // 고정 10개

@Override
public MemberResDTO.RecommendedDevelopersRes getRecommendedDevelopers(
        Member member,
        Long projectId
) {
    // 1. projectId 필수 체크
    if (projectId == null) {
        throw new ProjectException(PROJECT_ID_REQUIRED);
    }

    // 2. 프로젝트 임베딩 존재 확인
    ProjectEmbedding projectEmbedding = projectEmbeddingRepository.findByProjectId(projectId)
        .orElseThrow(() -> new ProjectException(PROJECT_EMBEDDING_NOT_FOUND));

    if (projectEmbedding.getEmbedding() == null) {
        throw new ProjectException(PROJECT_EMBEDDING_NOT_READY);
    }

    // 3. 복합 점수 기반 개발자 추천 쿼리 실행 (상위 10명 고정)
    // 쿼리 결과: [member_id, nickname, image, body, similarity_score, techstack_score,
    //            domain_score, total_score, similarity_score_percent,
    //            techstack_score_percent, domain_match]
    List<Object[]> results = reportEmbeddingRepository
        .findRecommendedDevelopersForProject(projectId, RECOMMEND_LIMIT);

    if (results.isEmpty()) {
        return MemberResDTO.RecommendedDevelopersRes.builder()
            .projectId(projectId)
            .developers(List.of())
            .count(0)
            .build();
    }

    // 4. N+1 방지: memberIds 추출 후 기술스택 한 번에 조회
    List<Long> memberIds = results.stream()
        .map(row -> ((Number) row[0]).longValue())
        .toList();

    // IN 쿼리로 기술스택 한 번에 조회
    List<DevTechstack> allTechstacks = devTechstackRepository.findAllByMemberIdIn(memberIds);

    // memberId별로 그룹핑
    Map<Long, List<DevTechstack>> techstackMap = allTechstacks.stream()
        .collect(Collectors.groupingBy(dt -> dt.getMember().getId()));

    // 5. 결과 변환 (추가 DB 조회 없음)
    List<MemberResDTO.DeveloperDTO> developerDTOs = results.stream()
        .map(row -> {
            Long memberId = ((Number) row[0]).longValue();
            String nickname = (String) row[1];
            String image = (String) row[2];
            String body = (String) row[3];
            Double totalScore = ((Number) row[7]).doubleValue();
            Double similarityScorePercent = ((Number) row[8]).doubleValue();
            Double techstackScorePercent = ((Number) row[9]).doubleValue();
            Boolean domainMatch = (Boolean) row[10];

            List<DevTechstack> devTechstacks = techstackMap.getOrDefault(memberId, List.of());

            return MemberResDTO.DeveloperDTO.builder()
                .memberId(memberId)
                .nickname(nickname)
                .image(image)
                .body(body)
                .techstacks(TechstackConverter.toTechstackDTOList(devTechstacks))
                .totalScore(Math.round(totalScore * 10) / 10.0)
                .similarityScorePercent(Math.round(similarityScorePercent * 10) / 10.0)
                .techstackScorePercent(Math.round(techstackScorePercent * 10) / 10.0)
                .domainMatch(domainMatch)
                .build();
        })
        .toList();

    return MemberResDTO.RecommendedDevelopersRes.builder()
        .projectId(projectId)
        .developers(developerDTOs)
        .count(developerDTOs.size())
        .build();
}
```

> **N+1 해결**: `devTechstackRepository.findAllByMemberIdIn(memberIds)`로 IN 쿼리 사용하여 기술스택을 한 번에 조회합니다.

#### Step 3: DevTechstackRepository에 IN 쿼리 메서드 추가

**파일**: `DevTechstackRepository.java`

```java
public interface DevTechstackRepository extends JpaRepository<DevTechstack, DevTechstack.DevTechstackId> {

    // 기존 메서드
    List<DevTechstack> findAllByMember(Member member);

    // 추가: N+1 방지용 IN 쿼리
    @Query("SELECT dt FROM DevTechstack dt JOIN FETCH dt.techstack WHERE dt.member.id IN :memberIds")
    List<DevTechstack> findAllByMemberIdIn(@Param("memberIds") List<Long> memberIds);
}
```

#### Step 4: 의존성 확인

**파일**: `MemberQueryServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class MemberQueryServiceImpl implements MemberQueryService {

    // 기존 의존성
    private final MemberRepository memberRepository;
    private final DevTechstackRepository devTechstackRepository;
    // ...

    // 추가 의존성
    private final ReportEmbeddingRepository reportEmbeddingRepository;
    private final ProjectEmbeddingRepository projectEmbeddingRepository;
}
```

---

### 프론트엔드 연동

#### 개발자 추천 API 호출 예시

```javascript
// 1. PM의 프로젝트 목록 조회 (내 프로젝트 선택 필터용)
const myProjects = await fetch('/api/v1/projects/my');

// 2. 프로젝트 선택 후 개발자 추천 (상위 10명 고정 반환)
const response = await fetch('/api/v1/members/recommend?projectIds=' + selectedProjectId);

// 3. 응답 데이터
{
  "projectId": 1,
  "developers": [
    {
      "memberId": 123,
      "nickname": "개발자A",
      "image": "https://...",
      "body": "3년차 백엔드 개발자",
      "techstacks": [...],
      "totalScore": 83.0,
      "similarityScorePercent": 80.0,
      "techstackScorePercent": 75.0,
      "domainMatch": true
    }
  ],
  "count": 10
}
```

#### 프로젝트 추천 API 호출 예시

```javascript
// 필터 적용된 프로젝트 추천 (상위 10개 고정 반환)
const response = await fetch('/api/v1/projects/recommend?' + new URLSearchParams({
  projectFields: 'WEB,MOBILE',
  categories: 'FINTECH',
  durationRanges: 'ONE_TO_THREE,THREE_TO_SIX'
}));

// 응답 데이터
{
  "projects": [
    {
      "projectId": 1,
      "title": "AI 협업 플랫폼",
      "projectField": "WEB",
      "category": "FINTECH",
      "totalScore": 85.0,
      "similarityScorePercent": 90.0,
      "techstackScorePercent": 75.0,
      "domainMatch": true
    }
  ],
  "count": 10
}
```

---

## 체크리스트

### 개발자 추천 (기존 API 수정)

- [ ] `MemberResDTO` 수정
  - [ ] `DeveloperDTO`에 점수 필드 추가 (`totalScore`, `similarityScorePercent`, `techstackScorePercent`, `domainMatch`)
  - [ ] `RecommendedDevelopersRes` DTO 신규 추가 (List 반환, 페이지네이션 제거)
- [ ] `MemberReqDTO.RecommendDeveloperDTO` 수정
  - [ ] `projectIds` 파라미터만 유지 (page, size, 기타 필터 제거)
- [ ] `MemberQueryServiceImpl.getRecommendedDevelopers()` 신규 구현
  - [ ] 고정 10명 반환 (RECOMMEND_LIMIT = 10)
  - [ ] 복합 점수 기반 추천 쿼리 연동
- [ ] `ReportEmbeddingRepository`에 복합 점수 기반 검색 메서드 추가
  - [ ] `findRecommendedDevelopersForProject()` - 복합 점수 계산 쿼리
  - [ ] `findMemberRootPositions()` - 개발자 주 포지션 조회

### 프로젝트 추천 (기존 API 수정)

- [ ] `ProjectResDTO` 수정
  - [ ] `RecommendedProjectSummary`에 점수 필드 수정 (기존 0~5 스케일 → 100점 만점)
  - [ ] `RecommendedProjectsRes` 수정 (PagedResponse → List, 페이지네이션 제거)
- [ ] `ProjectReqDTO.RecommendProjectsReq` 수정
  - [ ] 필터만 유지 (page, size 제거)
- [ ] `ProjectQueryServiceImpl.getRecommendedProjects()` 수정
  - [ ] 고정 10개 반환 (RECOMMEND_LIMIT = 10)
  - [ ] 복합 점수 기반 추천 쿼리 연동
  - [ ] 필터 처리 (프로젝트 유형, 도메인, 기술스택, 예상 기간)
- [ ] `ProjectEmbeddingRepository`에 복합 점수 기반 검색 메서드 추가
  - [ ] `findRecommendedProjectsForMember()` - 복합 점수 계산 쿼리
  - [ ] `findRecommendedProjectsWithFilters()` - 필터 조건 포함

### 테스트

- [ ] 개발자 추천 API 테스트
  - [ ] `projectIds` 파라미터 전달 시 복합 점수 기반 추천 확인
  - [ ] 점수 필드 응답 확인 (`totalScore`, `similarityScorePercent`, `techstackScorePercent`, `domainMatch`)
  - [ ] 상위 10명 고정 반환 확인
- [ ] 프로젝트 추천 API 테스트
  - [ ] 필터 파라미터 동작 확인 (복수 선택, 미선택 시 전체)
  - [ ] 점수 필드 응답 확인
  - [ ] 상위 10개 고정 반환 확인

---

## 핵심 설계 결정

### 왜 기존 Techstack 계층 구조를 활용하는가?

1. **기존 데이터 활용**: `DevTechstack` 매핑 테이블에 이미 개발자의 기술스택 정보가 저장되어 있음
2. **스키마 변경 최소화**: Member 엔티티에 새 필드 추가 불필요
3. **일관성 유지**: 기술스택과 포지션 정보가 한 곳에서 관리됨
4. **유연성**: 개발자가 여러 Root 포지션(BACKEND + FRONTEND)을 가질 수 있음

### Techstack 계층 구조 예시

```
BACKEND (Root, parent_stack = NULL)
├── JAVA (parent_stack = BACKEND)
├── PYTHON (parent_stack = BACKEND)
├── SPRINGBOOT (parent_stack = BACKEND)
└── NODEJS (parent_stack = BACKEND)

FRONTEND (Root, parent_stack = NULL)
├── JAVASCRIPT (parent_stack = FRONTEND)
├── REACT (parent_stack = FRONTEND)
└── VUEJS (parent_stack = FRONTEND)

INFRA (Root, parent_stack = NULL)
├── AWS (parent_stack = INFRA)
├── DOCKER (parent_stack = INFRA)
└── KUBERNETES (parent_stack = INFRA)
```

### TechName ↔ ProjectPart 매핑 고려사항

| 상황 | 처리 방법 |
|------|----------|
| BACKEND → BACKEND | 직접 매핑 |
| FRONTEND → FRONTEND | 직접 매핑 |
| INFRA → INFRA | 직접 매핑 |
| 기술스택 없음 | 파트 필터 없이 벡터 유사도만으로 검색 |

> **참고**: TechName의 Root(BACKEND, FRONTEND, INFRA)는 ProjectPart와 1:1 직접 매핑됩니다.

---

## 참고 문서

- [pgvector 공식 문서](https://github.com/pgvector/pgvector)
- [Hibernate Vector 지원](https://hibernate.org/orm/releases/6.6/)
