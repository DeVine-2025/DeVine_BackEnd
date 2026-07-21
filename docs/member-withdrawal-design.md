# 회원 탈퇴 기능 설계 가이드

본 문서는 devine 프로젝트에서 회원 탈퇴 기능을 구현하기 전에 반드시 검토해야 할 사항을 정리한 설계 문서입니다. 코드 작성 전에 팀과 정책을 합의하기 위한 기준 자료로 사용합니다.

---

## 0. 현재 코드베이스 상태 요약

| 항목 | 현황 | 비고 |
| --- | --- | --- |
| 회원 상태 enum | `MemberStatus { ACTIVE, DELETED }` | P0에서 레거시 `INACTIVE` 제거 완료 |
| 상태 컬럼 | `member.used` (NOT NULL, length 20) | 모든 조회에서 수동 필터링 |
| 탈퇴 메서드 | `Member.withdraw()` → `used = DELETED` | `MemberCommandServiceImpl.withdraw()`도 동일 동작만 함 |
| 탈퇴 시각 컬럼 | **없음** | `BaseEntity`의 `updatedAt`은 다른 업데이트로 덮어쓰임 |
| Clerk 연동 | `ClerkApiClient`에서 GitHub OAuth 토큰 조회만 구현 | 사용자 삭제 호출 없음 |
| Soft delete 자동화 | `@SQLRestriction`/`@Where` 미사용 | JPQL·native SQL에 직접 `used = 'ACTIVE'` 작성 |
| 관련 raw SQL | `SseController.java:65`, `websocket-chat-guide.md` 등 | 누락 위험 존재 |
| 기존 스케줄러 | 03:00 이미지 정리, 월 00:00 view count, 30분 임베딩 재시도, 30초 presence | 신규 배치 시간 충돌 검토 필요 |
| 문서 정합성 | `docs/redis-usage-guide.md:80`은 "탈퇴 시 INACTIVE 처리"라고 기재 | 실제 코드는 `DELETED` |

---

## 1. Clerk 사용자 삭제와 법적 검토

### 1.1 Clerk Backend API
Clerk은 사용자 삭제 엔드포인트를 제공합니다.

```
DELETE https://api.clerk.com/v1/users/{user_id}
Authorization: Bearer <CLERK_SECRET_KEY>
```

현재 `ClerkApiClient`에는 GitHub OAuth 토큰 조회 메서드만 존재하므로, 탈퇴 시점에 위 엔드포인트를 호출하는 메서드를 추가해야 합니다. 호출은 다음 원칙을 지킵니다.

- DB의 soft delete 처리와 Clerk 호출은 **하나의 비즈니스 트랜잭션 안에서 묶지 말 것**. Clerk 호출은 외부 IO이므로 DB 트랜잭션을 길게 잡으면 안 됩니다. DB 커밋 후 Clerk을 호출하는 순서를 권장합니다.
- Clerk 호출 실패에 대비해 재시도 큐(혹은 outbox 패턴)로 관리하는 것이 안전합니다. 단순히 즉시 호출 후 실패를 무시하면 법적 의무를 지키지 못합니다.
- 멱등성을 보장해야 합니다. 같은 `clerkId`로 두 번 호출되어도 안전해야 합니다(404는 정상 응답으로 처리).

### 1.2 법적 의무
- **개인정보보호법 제21조(개인정보의 파기)**: 보유 목적이 달성되거나 동의를 철회한 경우 지체 없이 파기해야 합니다. Clerk에 저장된 이메일·OAuth identity 등도 모두 개인정보에 해당합니다.
- **GDPR Article 17 (Right to erasure)**: EU 사용자가 있다면 동일하게 적용됩니다.
- **예외 보존 의무**: 전자상거래법 등에 따라 결제·계약 기록은 일정 기간 보관해야 합니다. devine은 결제 도메인이 있으므로 탈퇴 시 결제 내역의 분리 보관 정책이 필요합니다.

> 결론: Clerk 사용자 삭제는 선택이 아니라 의무입니다. 누락 시 행정처분/과징금 리스크가 있습니다.

### 1.3 동의 철회 후의 식별값
법적으로 "회원 식별이 불가능하도록" 처리해야 하므로, Clerk 삭제와 함께 DB의 다음 컬럼들을 익명화/NULL 처리하는 것을 권장합니다.

- `clerk_id` → NULL 또는 `deleted-{uuid}`로 치환 (unique 제약 충돌 회피)
- `name`, `nickname`, `address`, `image`, `body`, `github_username` → NULL 또는 마스킹
- `member_id`는 외래키 무결성 때문에 유지 가능

이 처리는 hard delete를 하지 않고 soft delete만 유지하는 경우에도 필수입니다.

---

## 2. Flag(상태 컬럼) 방식 vs 별도 테이블

### 2.1 현황
이미 `member.used` 컬럼 기반의 flag 방식이 도입되어 있습니다. 새 테이블을 만드는 대신 현재 구조를 보강하는 방향이 자연스럽습니다.

### 2.2 현재 구조의 약점
1. **누락 위험**: JPQL/QueryDSL/native SQL 어디서나 `used = 'ACTIVE'` 조건을 직접 적어야 합니다. 새 쿼리를 추가하는 사람이 빠뜨리면 탈퇴 회원이 노출됩니다.
2. **Native SQL 산재**: `SseController.java:65`, `websocket-chat-guide.md`의 SQL은 자동화된 조건 적용 대상이 아니므로 별도 점검이 필요합니다.
3. **문서·코드 불일치**: `docs/redis-usage-guide.md:80`은 "탈퇴 시 INACTIVE 처리"라고 기재되어 있으나 실제 코드는 `DELETED`로 동작합니다.
4. **`INACTIVE`는 레거시 잔재**: 과거에는 회원가입 플로우가 "빈 user row 선생성(`INACTIVE`) → 가입 완료 시 `ACTIVE`로 전환" 형태였습니다. 현재 가입 플로우는 이 단계를 사용하지 않으며, **P0에서 enum과 DB CHECK 제약에서 모두 제거되었습니다**(`V20260409000000__remove_member_status_inactive.sql`).

### 2.3 별도 테이블 방식과의 비교

| 항목 | flag (현행) | 별도 `withdrawn_member` 테이블 |
| --- | --- | --- |
| 마이그레이션 비용 | 낮음 (이미 도입) | 높음 (이관 + 외래키 처리) |
| 조회 누락 위험 | 높음 (수동 조건) | 낮음 (애초에 다른 테이블) |
| 외래키 영향 | 없음 | 프로젝트·채팅 등 fk 처리 필요 |
| 통계/감사 | 한 테이블 안에서 가능 | 별도 join 필요 |
| 권장 여부 | **devine에 적합** | 외래키가 적은 단순 구조에 적합 |

devine은 회원과 연결된 도메인이 많아(프로젝트, 채팅, 북마크, 이미지, 카테고리, 약관 등) **flag 방식을 유지하면서 자동화 장치를 보강하는 것이 비용 대비 효과가 높습니다.**

---

## 3. 스케줄러 운영 시 리소스 검토

### 3.1 기존 스케줄 시간표

| 스케줄러 | 시각 | 위치 |
| --- | --- | --- |
| `ImageCleanupScheduler` | 매일 03:00 | `devine-api/global/scheduler` |
| `WeeklyViewCountResetScheduler` | 매주 월 00:00 (Asia/Seoul) | `devine-api/global/scheduler` |
| `EmbeddingRetryScheduler` | 매 30분 | `devine-api/global/scheduler` |
| `SseHeartbeatScheduler` | 설정값 fixedRate | `devine-realtime/global/scheduler` |
| `ChatPresenceCleanupScheduler` | 30초 fixedRate | `devine-realtime/infrastructure/chat/presence` |
| `ChatPresenceRefreshScheduler` | 10초 fixedRate | `devine-realtime/infrastructure/chat/presence` |

### 3.2 신규 탈퇴 배치 시각 권장
- 00:00은 월요일에 view count 리셋과 충돌하므로 피합니다.
- 03:00은 이미지 정리와 겹칩니다.
- **04:00 또는 04:30(Asia/Seoul)** 권장. 새벽 트래픽 저점이며 다른 배치와 겹치지 않습니다.

### 3.3 Spring `@Scheduled`의 기본 동작
- 기본 `TaskScheduler`는 **single-thread** 입니다. 한 작업이 길어지면 다음 작업이 대기합니다.
- 병렬화하려면 `ThreadPoolTaskScheduler`의 `pool-size`를 명시적으로 늘려야 하며, 그 경우 DB 커넥션 풀과의 경합을 함께 고려해야 합니다.
- 회원 삭제는 외래키 cascade가 무거울 수 있으므로 **병렬 풀로 키우기보다는 시간 분산 + 청크 처리**가 더 안전합니다.

### 3.4 분산 환경 주의
운영 인스턴스가 2개 이상이라면 모든 인스턴스에서 동일 배치가 동시에 실행됩니다. 다음 중 하나로 중복 실행을 막아야 합니다.
- ShedLock 등 분산 락
- 배치 전용 인스턴스/프로파일 분리 (`@Profile("batch")`)
- DB row lock 기반 가드

---

## 4. `@SQLRestriction`(구 `@Where`) 도입 검토

### 4.1 어노테이션 변경 사항
- Hibernate 6.3+ 이후 `@Where`은 deprecated되었고 **`@SQLRestriction`** 으로 대체되었습니다. 기능은 동일합니다.
- 사용 예시 (개념 설명용, 실제 도입 시 코드 리뷰 필요):
  ```java
  @Entity
  @SQLRestriction("used = 'ACTIVE'")
  public class Member extends BaseEntity { ... }
  ```

### 4.2 장점
- 모든 JPA 쿼리(JPQL/Criteria/Spring Data)에 자동으로 조건 적용 → **누락 위험이 사실상 사라짐**
- `@OneToMany`, `@ManyToOne` fetch에도 자동 적용
- 코드 리뷰 시 "이 쿼리에 used 조건 빠졌어요"라는 지적이 사라짐

### 4.3 단점/주의
- **Native SQL에는 적용되지 않습니다.** `SseController.java:65` 등은 그대로 수동 조건이 필요합니다.
- 관리자 페이지에서 탈퇴 회원까지 조회해야 하는 경우 우회 수단이 필요합니다. Hibernate `@FilterDef`/`@Filter`를 함께 도입하면 on/off가 가능합니다.
- 연관관계 join에도 조건이 강제되므로, 탈퇴자가 작성한 채팅/프로젝트가 의도치 않게 사라질 수 있습니다. 이 동작이 비즈니스 요구와 맞는지 도메인별로 확인해야 합니다.

### 4.4 성능
- 단순히 `WHERE used = 'ACTIVE'`가 추가되는 것이라 SQL 자체 비용은 무시할 만합니다.
- 단, **`member.used`(또는 `(used, ...)` 복합) 인덱스가 반드시 있어야** 합니다. 없으면 회원 수가 늘어났을 때 풀 스캔이 발생할 수 있습니다.
- 인덱스 추가 시 cardinality가 낮은(ACTIVE가 99%) 컬럼이므로, 단독 인덱스보다 자주 쓰이는 조건과의 복합 인덱스가 더 효과적입니다.

### 4.5 도입 전 체크리스트
- [ ] 관리자/통계 화면에서 탈퇴 회원 조회 요건 정리
- [ ] Native SQL 사용처 전수 조사 및 수동 조건 보강
- [ ] `member.used` 관련 인덱스 점검
- [ ] 연관관계 fetch에 미치는 영향 도메인별 검토
- [ ] `@FilterDef`/`@Filter`로 우회 경로 확보 여부 결정

---

## 5. 탈퇴 시각(`deletedAt`) 컬럼 추가

### 5.1 추가 권장 이유
1. **유예 후 hard delete 배치의 기준 컬럼**으로 필수입니다(§6 참조).
2. **법적 처리 기록 의무** 충족: 언제 파기 처리를 시작했는지 입증 자료가 됩니다.
3. **CS 대응**: "언제 탈퇴했는지" 질의에 즉시 답할 수 있어야 합니다.
4. `BaseEntity.updatedAt`은 다른 업데이트로 덮어쓰이므로 부적합합니다.

### 5.2 컬럼 설계 제안
- 컬럼명: `deleted_at`, 타입 `TIMESTAMP NULL`
- 기본값 NULL, 탈퇴 시점에만 채움
- 인덱스: `idx_member_deleted_at` (배치가 `deleted_at < ?`로 스캔하므로 필수)
- 향후 부분 인덱스 가능: `WHERE used = 'DELETED'`

### 5.3 마이그레이션 주의
- 기존 회원에는 NULL을 그대로 둡니다.
- Flyway 스크립트로 컬럼 추가 + 인덱스 생성을 같은 마이그레이션으로 묶습니다.

---

## 6. 일괄 삭제 배치 정책

### 6.1 권장 패턴: Soft delete + 유예 후 Hard delete
1. 사용자가 탈퇴 요청 → 즉시 다음을 수행
   - `used = DELETED`
   - `deleted_at = now()`
   - 개인정보 컬럼 익명화/NULL 처리(§1.3)
   - Clerk 사용자 삭제 호출(실패 시 재시도 큐)
   - 세션/토큰 무효화(로그아웃)
2. 매일 04:00 배치가 `deleted_at < now() - INTERVAL N DAY` 인 회원만 hard delete
   - 권장 N 값: 7~30일 (CS 복구 여지 + 법적 파기 의무 균형)
   - 한국 개인정보보호법은 "지체 없이"를 원칙으로 하므로 N은 합리적으로 짧게 유지

### 6.2 한꺼번에 전체 삭제를 피해야 하는 이유
- devine은 회원에 연결된 외래키가 많습니다(프로젝트, 채팅, 북마크, 이미지, 카테고리 매핑, 약관 등). cascade 비용이 매우 큽니다.
- 긴 트랜잭션이 락을 잡으면 새벽 시간이라도 서비스 영향이 발생합니다.
- 메모리 OOM 위험.

### 6.3 청크 처리 권장
- 한 번에 100~500건씩 페이징하여 처리하고, 청크마다 별도 트랜잭션으로 커밋합니다.
- Spring Batch까지 도입할 필요는 없습니다. `@Scheduled` + 페이징 루프로 충분합니다.
- 실패한 회원은 별도 테이블/로그에 적재하여 재시도합니다.

### 6.4 Cascade 영향 도메인 점검
다음 도메인이 회원 삭제 시 함께 처리되어야 합니다. 도메인별로 cascade·익명화 정책을 명시해야 합니다.

- 프로젝트, 프로젝트 임베딩
- 채팅 메시지, 채팅방
- 북마크
- 이미지(S3 객체 포함)
- 카테고리 매핑
- 약관 동의 이력 (법적 보존 기간 확인)
- DevTechstack, GitRepoUrl, Contact
- Report 관련 데이터
- Redis에 캐시된 회원 관련 키 (`docs/redis-usage-guide.md` 참고)

### 6.5 유예 기간 동안의 정책
- 로그인 차단: 클라이언트에 "탈퇴 처리 중" 안내
- 노출 차단: 다른 사용자에게 보이지 않음 (`@SQLRestriction`이 적용되면 자동)
- 복구 정책: CS를 통해서만 가능 / 자동 복구 미제공 등 명문화

---

## 7. 구현 우선순위 및 커밋 분할

### 7.1 우선순위 결정 원칙
- **앞쪽 커밋일수록 영향 범위가 작고 되돌리기 쉬움.** 뒤로 갈수록 영향이 커집니다.
- **앞 커밋은 뒤 커밋의 전제 조건**이 되도록 배치합니다. 한 PR이 reject되어도 앞 커밋들은 살아남을 수 있어야 합니다.
- **`@SQLRestriction`은 마지막**입니다. 영향 범위가 가장 넓고, 도입 후 모든 쿼리 동작이 바뀌므로 다른 변경과 섞이면 회귀 원인 추적이 어렵습니다.
- **Hard delete 배치는 soft delete + Clerk 삭제가 검증된 이후**에 도입합니다. 검증 안 된 채 배치를 돌리면 복구 불가능한 사고가 납니다.
- 한 커밋에는 한 가지 의미만 담습니다. devine 컨벤션(`[FEATURE]`, `[FIX]`, `[REFACTOR]`, `[BUILD]`)을 따릅니다.

### 7.2 커밋 순서 (P0 → P5)

각 항목은 독립 커밋(또는 PR) 단위입니다. 앞 커밋이 머지된 후 다음으로 진행합니다.

---

#### **P0. `[FIX] 회원 상태 관련 문서/레거시 정합성 정리`**
> 실제 동작 변경 없음. 가장 안전한 시작점.

- `docs/redis-usage-guide.md:80`의 "탈퇴 시 `INACTIVE` 처리" → "탈퇴 시 `DELETED` 처리"로 수정
- `MemberStatus.INACTIVE`를 enum에서 제거하고, DB CHECK 제약을 새 Flyway 마이그레이션으로 갱신
- `Member.withdraw()`와 `MemberCommandServiceImpl.withdraw()`의 "테스트 코드용" 주석 정리 (실제 탈퇴용으로 사용될 예정임을 명시)

**왜 P0?** 코드 동작 영향 0, 리뷰 부담 최소, 이후 작업의 인지 부조화를 미리 제거합니다.

---

#### **P1. `[FEATURE] 회원 탈퇴 시각(deletedAt) 컬럼 추가`**
> Flyway + 엔티티 필드 추가만. 비즈니스 로직 변경 없음.

- Flyway 마이그레이션:
  - `member.deleted_at TIMESTAMP NULL` 컬럼 추가
  - `idx_member_deleted_at` 인덱스 추가 (배치 스캔용)
- `Member` 엔티티에 `deletedAt` 필드 추가 (NULL 허용)
- `Member.withdraw()`에서 `this.deletedAt = LocalDateTime.now()` 세팅 추가
- 단위 테스트: `withdraw()` 호출 후 `deletedAt`이 채워지는지 검증

**왜 P1?** 이후 단계(익명화, Clerk 호출, 배치)가 모두 이 컬럼을 전제로 합니다. 가장 먼저 머지되어야 합니다. Flyway 단독 변경이라 롤백도 단순합니다.

**롤백 전략**: 컬럼 추가는 비파괴적이라 안전합니다. 문제가 생기면 후속 마이그레이션으로 컬럼을 제거할 수 있습니다.

---

#### **P2. `[FEATURE] Clerk 사용자 삭제 API 클라이언트 추가`**
> 외부 API 호출 메서드만 추가. 호출처 없음(다음 커밋에서 연결).

- `ClerkApiClient`에 `deleteUser(String clerkUserId)` 메서드 추가
  - `DELETE https://api.clerk.com/v1/users/{user_id}`
  - 404는 정상 응답으로 처리(멱등성)
  - 4xx/5xx 예외 매핑
- 신규 `AuthErrorReason` 코드 추가 (`CLERK_USER_DELETE_FAILED` 등)
- 단위 테스트: MockRestServiceServer로 200/404/5xx 시나리오 검증

**왜 P2?** 이 메서드가 없으면 P3가 동작하지 않습니다. 호출처 없이 메서드만 추가하므로 리뷰 범위가 좁습니다. P1과 의존성이 없어 P1과 병렬 진행도 가능하지만, 충돌 회피를 위해 순차 진행을 권장합니다.

---

#### **P3. `[FEATURE] 회원 탈퇴 처리 로직 구현`**
> 이 PR이 핵심 비즈니스 로직입니다. 가장 신중하게 리뷰해야 합니다.

- `MemberCommandServiceImpl.withdraw()` 재구현
  1. DB 트랜잭션 안: `used = DELETED`, `deletedAt = now()`, 개인정보 컬럼 익명화
     - `clerkId` → `deleted-{uuid}` (unique 제약 충돌 회피)
     - `name`, `nickname`, `address`, `image`, `body`, `githubUsername` → NULL
  2. DB 커밋 후: `clerkApiClient.deleteUser(originalClerkId)` 호출
  3. Clerk 호출 실패 시 로그 + 별도 재시도 테이블에 적재 (간단 버전 우선)
  4. 세션/토큰 무효화
- 컨트롤러 엔드포인트 추가 (`DELETE /api/v1/members/me` 등)
- Redis 캐시 정리 (`docs/redis-usage-guide.md` 참고)
- 통합 테스트: 탈퇴 후 조회 차단, Clerk mock 호출 검증, 익명화 검증

**왜 P3?** P1(컬럼)과 P2(Clerk API)가 모두 필요합니다. 이 단계까지 머지되면 **사용자 입장에서의 탈퇴 기능은 동작합니다.** Hard delete 없이도 법적 의무(Clerk 삭제 + 익명화)는 충족됩니다.

**주의**: outbox 패턴은 P3에서는 도입하지 말고, 단순 재시도 테이블 + 로그로 시작합니다. 과한 설계는 후속 PR에서 도입합니다.

---

#### **P4. `[FEATURE] 탈퇴 회원 Hard Delete 배치 추가`**
> P3가 운영에서 안정적으로 동작함을 확인한 후 진행합니다.

- 신규 스케줄러 작성: 매일 **04:00 Asia/Seoul** (기존 스케줄과 충돌 없음)
- `deleted_at < now() - INTERVAL N DAY` 조건으로 페이징 조회
  - 청크 크기 100~500
  - 청크마다 별도 트랜잭션 커밋
- Cascade 영향 도메인 사전 점검(§6.4 목록)
- 실패한 회원은 별도 로그/테이블에 적재
- 분산 락 검토: 운영 인스턴스가 2개 이상이면 ShedLock 도입
- 통합 테스트: 청크 처리, cascade 동작, 실패 회원 격리

**왜 P4?** P3 머지 후 최소 1~2주는 운영하면서 soft delete 동작을 관찰한 뒤 진행해야 합니다. 한번 hard delete가 실행되면 복구 불가능합니다.

**롤백 전략**: 배치 컴포넌트는 `@ConditionalOnProperty`로 감싸 운영 토글이 가능하도록 합니다.

---

#### **P5. `[REFACTOR] @SQLRestriction 도입 및 native SQL 보강`**
> 가장 영향 범위가 넓습니다. 단독 PR로 진행합니다.

- `Member` 엔티티에 `@SQLRestriction("used = 'ACTIVE'")` 적용
- `member.used` 관련 인덱스 점검 및 복합 인덱스 추가
- Native SQL 사용처 전수 보강 (`SseController.java:65`, `websocket-chat-guide.md` 등)
- 관리자 조회 우회 경로 확보 (`@FilterDef`/`@Filter` 또는 별도 native query)
- 모든 회원 관련 통합 테스트 재실행
- 연관관계 fetch 영향 검증 (탈퇴자가 작성한 채팅·프로젝트 노출 정책)

**왜 P5(맨 마지막)?** 모든 JPA 쿼리의 동작이 바뀝니다. P4까지 회원 탈퇴 기능이 완성되어 있으면 이 PR은 "조회 안전망 강화" 목적이 됩니다. 만약 이 PR에 문제가 생겨도 P0~P4는 영향받지 않고 살아있습니다.

**도입을 안 할 수도 있습니다.** P0~P4만으로도 회원 탈퇴 기능 자체는 완성됩니다. P5는 "수동 필터링 누락 위험을 시스템적으로 막고 싶다"는 별개 목적이므로, 팀이 비용 대비 효과를 다시 평가한 후 결정합니다.

---

### 7.3 우선순위 요약 표

| 순위 | 커밋 | 영향 범위 | 의존성 | 머지 후 효과 |
| --- | --- | --- | --- | --- |
| P0 | `[FIX] 문서/레거시 정합성 정리` | 문서·주석 | 없음 | 인지 부조화 제거 |
| P1 | `[FEATURE] deletedAt 컬럼 추가` | 스키마·엔티티 | P0 | 시각 기록 가능 |
| P2 | `[FEATURE] Clerk 삭제 API 클라이언트` | 인프라 | 없음 | API 호출 가능 |
| P3 | `[FEATURE] 회원 탈퇴 로직 구현` | 도메인·API | P1, P2 | **탈퇴 기능 사용 가능** |
| P4 | `[FEATURE] Hard Delete 배치` | 배치 | P3 + 운영 검증 | 자동 파기 |
| P5 | `[REFACTOR] @SQLRestriction 도입` | 전 도메인 쿼리 | P3 (선택) | 누락 방지 |

### 7.4 PR 분할 전략
- 각 우선순위(P0~P5)는 **별개 PR**로 만듭니다. 한 PR에 묶지 마세요.
- **P3은 PR 안에서도 커밋을 더 쪼개도 좋습니다**(예: 1) 익명화 로직, 2) 컨트롤러 엔드포인트, 3) Redis 정리, 4) 통합 테스트). 리뷰어가 단계별로 따라 읽을 수 있습니다.
- P4는 머지 직후 운영 토글을 끈 채로 배포하고, 로그만 관찰하다가 활성화하는 방식이 안전합니다.

### 7.5 테스트·운영 점검 체크리스트
각 PR에서 해당 항목을 만족시켜야 합니다.

- [ ] P1: `withdraw()` 호출 시 `deletedAt` 채워짐
- [ ] P2: Clerk 삭제 API 200/404/5xx mock 테스트 통과
- [ ] P3: 탈퇴 후 조회 차단, 익명화, Clerk 호출, Redis 정리, 세션 무효화
- [ ] P4: 청크 처리, cascade, 실패 격리, 분산 락
- [ ] P5: 전 도메인 회귀 테스트, 관리자 우회 경로, 인덱스 plan 확인

---

## 8. 미결정 항목 (팀 합의 필요)

- [ ] 유예 기간 N일 값 결정
- [ ] 결제·약관 동의 등 법적 보존 대상 컬럼/테이블 목록 확정
- [x] `MemberStatus.INACTIVE` 처리 방향: P0에서 enum과 DB CHECK 제약 모두에서 제거 완료
- [ ] 관리자 페이지의 탈퇴 회원 조회 필요 여부
- [ ] Clerk 호출 실패 시 재시도 메커니즘 (outbox vs 큐 vs 단순 재배치)
- [ ] 분산 락 도입 여부 (운영 인스턴스 수에 따라)
- [ ] 탈퇴 회원이 작성한 콘텐츠(채팅, 프로젝트)의 노출 정책
