# 회원 탈퇴 기능 (#273)

## 개요

회원이 서비스를 탈퇴할 수 있는 기능을 추가한다. 탈퇴는 **Soft Delete → Hard Delete** 2단계로 진행되며, 탈퇴 즉시 개인정보를 익명화하고 외부 인증(Clerk) 계정도 삭제한다.

---

## 1. DB 스키마 정리

회원 상태를 관리하는 `MemberStatus`에서 사용되지 않는 `INACTIVE` 값을 제거하고, `ACTIVE` / `DELETED` 두 상태만 허용하도록 DB 제약 조건을 변경한다.

```sql
ALTER TABLE member DROP CONSTRAINT member_used_check;
ALTER TABLE member ADD CONSTRAINT member_used_check CHECK (used IN ('ACTIVE', 'DELETED'));
```

이후 Hard Delete 배치에서 보관 기간을 판단할 수 있도록 `deleted_at` 컬럼과 인덱스를 추가한다.

```sql
ALTER TABLE member ADD COLUMN deleted_at timestamp(6) without time zone;
CREATE INDEX idx_member_deleted_at ON member (deleted_at);
```

---

## 2. Clerk 사용자 삭제 클라이언트

외부 인증 서비스인 Clerk에서 탈퇴 회원의 계정을 삭제하기 위한 API 클라이언트를 추가한다.

- `ClerkApiClient.deleteUser(clerkId)` — Clerk REST API `DELETE /users/{clerkId}` 호출
- **404 응답은 정상 처리** — 이미 삭제된 경우에도 멱등성을 보장
- 그 외 4xx/5xx 응답 시 `AuthErrorReason.CLERK_USER_DELETE_FAILED` 예외 발생

---

## 3. 회원 탈퇴 API

### 엔드포인트

```
DELETE /api/v1/members/me
```

로그인된 회원 본인만 호출할 수 있으며, 응답 코드는 `MemberSuccessCode.WITHDRAWN`이다.

### 처리 흐름

탈퇴 요청이 들어오면 하나의 트랜잭션 안에서 다음 작업을 수행한다.

**① PII 연관 데이터 삭제**

개인 식별이 가능한 연관 데이터를 FK 의존 순서의 역순으로 삭제한다. 모든 삭제는 JPQL/네이티브 bulk delete로 실행하여 N+1 문제를 방지한다.

```
report_embedding → dev_report → git_repo_url → contact → dev_techstack
```

| 삭제 대상 | 이유 |
|---|---|
| report_embedding, dev_report | 회원의 GitHub 분석 리포트 (개인 코드 분석 결과) |
| git_repo_url | 회원의 GitHub 레포지토리 URL |
| contact | 이메일, LinkedIn 등 연락처 |
| dev_techstack | 회원이 등록한 기술 스택 |

**② Member 익명화**

Member 엔티티의 개인정보 필드를 모두 제거하고 상태를 변경한다.

| 필드 | 변경값 |
|---|---|
| `used` | `DELETED` |
| `deletedAt` | 현재 시각 |
| `clerkId` | `deleted-{UUID}` (고유성 유지) |
| `name` | `null` |
| `nickname` | `"deleted"` |
| `address`, `image`, `body`, `githubUsername` | `null` |

**③ Clerk 계정 삭제 (비동기)**

트랜잭션 커밋 후 `MemberWithdrawnEvent`를 통해 비동기로 Clerk 사용자를 삭제한다.

- `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`
- DB 커밋이 성공한 뒤에만 외부 API를 호출하므로, DB 롤백 시 Clerk 계정이 삭제되지 않는다

---

## 4. Hard Delete 배치

Soft Delete된 회원의 DB 행을 보관 기간 경과 후 완전히 삭제하는 스케줄러이다.

### 활성화 조건

```yaml
member:
  hard-delete:
    enabled: true          # 기본 false (명시적으로 켜야 동작)
    retention-days: 30     # 보관 기간 (기본 30일)
    chunk-size: 100        # 한 번에 조회할 회원 수 (기본 100)
```

### 실행 주기

매일 04:00 KST (`@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")`)

### 처리 방식

1. `DELETED` 상태이고 `deletedAt < 현재 - 보관일수`인 회원을 청크 단위로 조회
2. 각 회원마다 **별도 트랜잭션**으로 FK 종속 테이블 11개를 네이티브 쿼리로 정리 후 member 행 삭제
3. payment, matching, project, chat 등 비즈니스 레코드가 남아 있으면 FK 위반이 발생하며, 이 경우 해당 회원은 **건너뛰고 로그를 남긴다**

### FK 삭제 순서

```
report_embedding
  → dev_report
    → git_repo_url
contact
dev_techstack
member_category
member_agreement
bookmark
member_report_credit
image
notification (receiver_id, sender_id 모두)
  → member (최종 삭제)
```

---

## 전체 흐름 요약

```
사용자 요청: DELETE /api/v1/members/me
  │
  ▼
┌─────────────────── 트랜잭션 ───────────────────┐
│                                                 │
│  1. PII 연관 데이터 bulk delete (FK 역순)       │
│  2. Member 개인정보 익명화 + DELETED 상태 전환   │
│                                                 │
└─────────────────── commit ─────────────────────┘
  │
  ▼ (AFTER_COMMIT, 비동기)
  3. Clerk 사용자 삭제 API 호출
```

```
Hard Delete 배치 (매일 04:00 KST)
  │
  ▼
  DELETED + 보관 기간 경과 회원 청크 조회
  │
  ▼ (회원별 개별 트랜잭션)
  11개 FK 테이블 네이티브 DELETE → member 행 삭제
  (비즈니스 레코드 잔여 시 skip + 로그)
```
