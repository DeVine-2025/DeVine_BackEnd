# 관리자 로그인(인증/인가 골격) 설계

- 작성일: 2026-07-19
- 상태: 승인 대기
- 범위: **관리자 인증/인가 골격까지.** 실제 관리자 기능 도메인(회원/신고/결제 관리 등)은 각각 별도 스펙으로 진행한다.

## 1. 배경과 문제 정의

기획서(SW명세서)는 관리자 로그인을 "이메일/비밀번호 입력 → 관리자 계정 테이블 조회 → 비밀번호 해시 검증 → 세션 토큰 발급 → 성공/실패 로그 + 5회 실패 잠금"으로 기술한다.

그러나 현재 애플리케이션의 인증 아키텍처는 이와 근본적으로 다르다.

- 앱은 **Clerk를 OAuth2 Resource Server로 사용**한다. Clerk가 JWT를 발급하고, Spring은 검증만 한다(`oauth2ResourceServer.jwt()`).
- **완전 STATELESS** 이다(`SessionCreationPolicy.STATELESS`). 서버 세션을 만들지 않는다.
- 모든 사용자는 `ClerkJwtAuthenticationConverter`에서 **`ROLE_USER`로 하드코딩**된다. DB에 역할(Role) 개념이 없다.
- 앱은 비밀번호를 **저장하지 않는다**(Clerk가 관리).

따라서 기획서를 문자 그대로 구현하면 두 번째 자체 인증 체계(비번 저장/검증, 세션, 잠금)를 새로 만드는 셈이 된다. 본 설계는 대신 **기존 Clerk 인증을 재사용하고 인가(authorization) 계층만 분리**하는 방향을 택한다.

## 2. 핵심 결정

| 항목 | 결정 | 근거 |
|---|---|---|
| 인증 방식 | Clerk 재사용. 관리자도 Clerk로 로그인, 백엔드는 JWT 검증만 | 기존 STATELESS/JWT 아키텍처와 정합. 두 번째 인증 체계 불필요 |
| 관리자 원천 | 우리 DB의 `admin` 테이블. `/admin/v1/**` 경로에서만 조회 | 관리자 지정/회수/레벨을 우리가 완전 통제, 기획서의 "관리자 계정 테이블" 부합 |
| 캐싱 | 없음 | admin 판별이 저트래픽 admin 경로에서만 발생하므로 인덱스 조회 1회로 충분(YAGNI). Redis에 인가 매핑 캐싱 시 발생하는 회수 지연 회피 |
| 권한 레벨 | 단일 ADMIN, 확장용 `level` 컬럼만 보유 | 현재 다단계 요구 없음(YAGNI). 스키마만 확장 대비 |
| 감사 로그 | 구조화 로그만. DB 감사 테이블은 미도입 | 코드베이스 전반이 로그만 사용. 감사 대상 mutating 기능이 아직 없음 |
| 최초 관리자 | env 부트스트랩 이메일 + 최초 접근 시 lazy seeding | `clerk_id`는 가입 후에야 생기고 이메일은 미리 앎. 환경별 분리, git에 비밀 없음 |
| 경로/패키지 | `com.umc.devine.admin.*`, URL `/admin/v1/<도메인>` | 사용자 지정 |

## 3. 아키텍처 — 인증은 공유, 인가는 분리

관리자와 일반 유저는 **같은 Clerk JWT로 인증**한다(발급자/JWKS 동일). 분리되는 것은 인가 계층이며, 이를 **별도 SecurityFilterChain**으로 구현한다.

```
요청 → JWT 검증(Clerk, 공통)
     ├─ /admin/v1/**  → [Admin Chain, @Order(1)]  ROLE_ADMIN 필요 → 아니면 403
     └─ 그 외          → [기존 Chain,  @Order(2)]  ROLE_USER (기존 그대로)
```

- **Admin Chain**: `securityMatcher("/admin/**")`, STATELESS 유지, `AdminJwtAuthenticationConverter` 사용, `.anyRequest().hasRole("ADMIN")`. CORS는 기존 소스 재사용.
- **기존 Chain**: `@Order(2)`만 부여, 내용 무변경. `/admin/**`는 Admin Chain이 선점하므로 간섭 없음.

## 4. 컴포넌트

### (a) `Admin` 엔티티 + Flyway 마이그레이션 — `devine-core`

테이블 `admin`:

| 컬럼 | 타입 | 비고 |
|---|---|---|
| `id` | PK | |
| `clerk_id` | varchar, unique | 최초 로그인 시 채워짐 |
| `email` | varchar, unique | |
| `level` | smallint (또는 enum) | 기본값 = ADMIN. 확장 대비 |
| `is_active` | boolean | 회수용 소프트 삭제 |
| `granted_by` | varchar, nullable | 추가한 관리자 clerk_id |
| `created_at` / `updated_at` | timestamp | 기존 BaseEntity 규약 따름 |

- 새 Flyway 파일: `V<타임스탬프>__<이슈번호>_add_admin_table.sql` (기존 명명 규약 준수).
- 엔티티는 기존 `@Entity`/`@Table(name = "admin")` 패턴을 따른다.

### (b) `AdminRepository` — `devine-core`

- `findByClerkIdAndIsActiveTrue(String clerkId)`
- `existsByClerkIdAndIsActiveTrue(String clerkId)`
- (부트스트랩용) `findByEmailAndIsActiveTrue(String email)`

### (c) `AdminAuthorizationService` — `devine-api` (`com.umc.devine.admin.auth.service`)

- `resolveAdmin(clerkId, email)`:
  1. 활성 admin row 있으면 반환.
  2. 없고 `email`이 **부트스트랩 목록**에 있으면 lazy insert(`clerk_id` + `email` + 최고 `level`) 후 반환.
  3. 둘 다 아니면 "관리자 아님"(빈 결과).
- DB 읽기/쓰기 로직을 이 서비스에 격리하여 시큐리티 계층은 위임만 하도록 한다(트랜잭션 경계 명확화).

### (d) `AdminJwtAuthenticationConverter` — `devine-api` (Admin Chain 전용)

- 기존 `ClerkJwtAuthenticationConverter`와 동일하게 `ClerkPrincipal` 구성.
- `AdminAuthorizationService.resolveAdmin(...)` 호출:
  - 관리자면 authorities에 `ROLE_ADMIN` 부여(+ level 정보 보존).
  - 아니면 `ROLE_ADMIN` 미부여 → `hasRole("ADMIN")`에서 403.

### (e) 관리자 인증 컨트롤러 — `devine-api` (`com.umc.devine.admin.auth.controller`)

- `GET /admin/v1/auth/me` → `{ clerkId, email, level }`.
  - 기획서 Output의 "인증 토큰(세션)"은 기존 Clerk JWT 그 자체이며, "권한 레벨"은 이 응답으로 충족.
- (선택) `GET /admin/v1/auth/health`.
- 실제 관리자 기능 도메인은 이번 스펙 범위 밖(각각 별도 스펙).

### (f) 감사 로그 — `devine-api`

- Admin Chain에 소형 `OncePerRequestFilter`(또는 인터셉터)를 두어 `clerk_id, method, path, status`를 구조화 로그로 기록.
- **403(권한 없는 접근)도 기록** → 기획서 "실패 로그" 요구를 부분 충족.
- 기존 `LoggingFilter` 스타일(SLF4J, 민감 헤더 마스킹) 준수.

### (g) 설정

- `admin.bootstrap-emails`: `application.yml`에서 `${ADMIN_BOOTSTRAP_EMAILS}` 바인딩, 콤마 구분.
- 운영 주의: 부트스트랩 이메일은 사실상 백도어이므로 prod에서는 최소(가능하면 1명)로 유지한다. 최초 등록 완료 후에는 목록을 비워도 테이블 row로 동작이 유지된다.
- 운영 주의(회수): 관리자를 회수(`is_active=false`)할 때는 `ADMIN_BOOTSTRAP_EMAILS`에서도 해당 이메일을 반드시 제거한다. **회수 우선** 정책상 목록에 남아 있어도 재활성화되지는 않지만(비활성 행이 있으면 재등록하지 않고 empty 반환), 재활성화는 수동 DB 작업이 되므로 목록 정리를 절차로 둔다.

### (h) 판정 견고성 (구현 세부)

- **clerk_id(sub)만 신뢰**: 관리자 판정과 부트스트랩은 오직 `clerk_id`(JWT의 sub, 위조 불가)로만 한다. 이메일/`email_verified` 같은 Clerk 클레임 의미론(예: `user.email_verified`가 primary 이메일에 바인딩되는지 불명확)에 권한 경계를 의존시키지 않는다. 부트스트랩은 `admin.bootstrap-clerk-ids`(env `ADMIN_BOOTSTRAP_CLERK_IDS`)에 있는 `user_xxx`로 최초 접근 시 seeding한다. `email`은 nullable이며 표시/감사용으로만 저장한다(클레임이 없어도 관리자 동작).
- **회수 우선**: `resolveAdmin`은 `clerk_id` 행을 활성 여부와 무관하게 조회한 뒤 활성 상태를 적용한다. 비활성(회수) 행이 있으면 부트스트랩 목록에 clerk_id가 남아 있어도 재등록하지 않고 empty를 반환한다.
- **동시성/멱등 seeding**: 최초 등록은 `INSERT ... ON CONFLICT DO NOTHING`(대상 미지정) 네이티브 upsert로 처리한다. `@Transactional` 안에서 UNIQUE 위반이 트랜잭션을 abort시키는 문제(→500)를 원천 차단하고, 동시 최초 접근/재진입에도 예외 없이 멱등하다. DO NOTHING은 행을 반환하지 않으므로 삽입 후 `clerk_id → email` 순으로 재조회하며, 그 결과에도 활성 판정을 적용한다.
- 사전 존재 확인은 `clerk_id`/`email`을 **분리된 두 쿼리**로 수행한다(단일 OR 쿼리는 두 UNIQUE가 서로 다른 행에 걸릴 때 다중행으로 터질 수 있음).
- CHECK 제약(`admin_level_check`)은 ON CONFLICT로 완화되지 않는다. `AdminLevel` 확장 시 마이그레이션으로 CHECK도 함께 갱신하지 않으면 CHECK 위반 → abort의 동일 실패 모드가 재현되므로, 마이그레이션에 경고 주석을 둔다.

## 5. 기획서 예외처리 매핑

| 기획서 요구 | 처리 위치 |
|---|---|
| 입력값 유효성, 이메일 조회, 비밀번호 해시 검증 | Clerk(프론트 로그인). 백엔드 관여 없음 |
| 5회 실패 잠금, 계정 존재 노출 방지(동일 메시지) | Clerk(brute-force protection, 로그인 UI) |
| 관리자 아닌 사용자의 `/admin/**` 접근 | 우리: 403 + 일반 메시지(관리자 존재 여부 비노출) |
| 토큰 없음/무효 | 우리: 401(기존 `CustomAuthenticationEntryPoint`) |
| 로그인 성공/실패 로그 | Clerk 감사 로그 + 우리: 관리자 접근/403 구조화 로그 |

## 6. 테스트 전략

- `AdminAuthorizationService` 단위 테스트: 활성 admin 반환 / lazy seeding / 비관리자 거부 / **회수(비활성) 관리자 거부(clerk_id·email 경로)**.
- `AdminRepository` 통합 테스트: 활성 여부 무관 조회 / **ON CONFLICT 멱등성(clerk_id·email 충돌 모두 예외 없음)**.
- `AdminAuthorizationService` 통합 테스트(실 Postgres): 부트스트랩 seeding / **회수된 부트스트랩 관리자 재접근 시 500 없이 empty**.
- Admin Chain 인가 테스트: 관리자 JWT → 200, 일반 JWT → 403, 토큰 없음 → 401(기존 Clerk 테스트 유틸 재사용).
- 회귀: admin chain 도입 후 기존 유저 경로가 영향받지 않는지 확인.

## 7. YAGNI로 제외한 것

- Redis 캐싱
- DB 감사 테이블
- 다단계 권한
- 자체 비밀번호/세션 인증
- 관리자 CRUD API(다음 스펙)
- 실제 관리자 기능 도메인(회원/신고/결제 관리 등)

## 8. 향후(별도 스펙) 졸업 조건

- 관리자가 **데이터를 변경하는 실제 기능**(회원 정지/삭제, 결제 취소 등)이 도입되는 시점에, 해당 변경 행위에 한정한 **DB 감사 테이블**을 도입한다(되돌릴 수 없는 행위의 추적은 파일 로그보다 DB가 안전).
- 다단계 권한이 실제로 필요해지면 `level` 컬럼을 기반으로 SUPER_ADMIN 등을 추가한다.