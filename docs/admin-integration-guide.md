# 관리자(Admin) 기능 연동 가이드

관리자 페이지용 백엔드 기능을 개발할 때 기존 관리자 인증/인가 골격에 연동하는 방법을 설명합니다.

> 일반 사용자 인증은 [auth-integration-guide.md](./auth-integration-guide.md)를 참고하세요. 이 문서는 **관리자 전용(`/admin/v1/**`)** 기능에 한정합니다.

---

## 핵심 개념 (먼저 읽어주세요)

- 관리자도 **일반 사용자와 동일하게 Clerk로 로그인**합니다. 별도 로그인 API가 없습니다. 프론트가 Clerk 세션 토큰(JWT)을 `Authorization: Bearer <token>`으로 보내면 백엔드가 검증합니다.
- **인증은 공유, 인가만 분리**됩니다. `/admin/v1/**` 경로는 별도 `SecurityFilterChain`이 담당하며 **`ROLE_ADMIN`을 요구**합니다.
- **"누가 관리자인가"는 오직 `clerk_id`(JWT의 `sub`, 위조 불가)로 판정**합니다. 이메일 등 다른 클레임에 권한을 의존시키지 않습니다.
- 따라서 **관리자 기능 개발자는 인증/인가를 신경 쓸 필요가 없습니다.** `/admin/v1/**` 아래에 컨트롤러를 만들면 자동으로 관리자만 접근할 수 있습니다.

---

## 빠른 시작 — 새 관리자 API 추가

`/admin/v1/**` 아래 컨트롤러를 만들면 **별도 설정 없이 자동으로 `ROLE_ADMIN`이 강제**됩니다(관리자 체인이 `/admin/**`를 담당).

```java
package com.umc.devine.admin.member.controller; // com.umc.devine.admin.<도메인>.controller

import com.umc.devine.admin.auth.security.AdminPrincipal;
import com.umc.devine.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/v1/members") // 반드시 /admin/v1/ 로 시작
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public ApiResponse<AdminMemberResDTO.ListDTO> getMembers(
            @AuthenticationPrincipal AdminPrincipal admin // 현재 관리자 정보
    ) {
        // 여기 도달했다는 것은 이미 ROLE_ADMIN이 검증됐다는 뜻
        String actorClerkId = admin.getClerkId();
        return ApiResponse.onSuccess(AdminMemberSuccessCode.LIST_OK, adminMemberService.getMembers());
    }
}
```

- **경로가 `/admin/v1/**`가 아니면** 관리자 체인이 아니라 일반 사용자 체인이 처리하므로 `ROLE_ADMIN`이 걸리지 않습니다. 반드시 접두사를 지키세요.
- 관리자 체인은 `/admin/v1/**` 아래 **모든 요청에 `hasRole("ADMIN")`**을 적용합니다. 별도의 `@PreAuthorize`는 필요 없습니다.

---

## 패키지 / 경로 컨벤션

| 구분 | 위치 | 예시 |
|------|------|------|
| URL | `/admin/v1/<도메인>` | `/admin/v1/members`, `/admin/v1/reports` |
| 컨트롤러/서비스/DTO | `devine-api` · `com.umc.devine.admin.<도메인>.*` | `com.umc.devine.admin.member.controller` |
| 엔티티/리포지토리 | `devine-core` · `com.umc.devine.admin.<도메인>.*` | `com.umc.devine.admin.entity.Admin` |
| Swagger 문서 인터페이스 | 컨트롤러와 같은 패키지 | `AdminMemberControllerDocs` |
| 성공 코드 enum | `com.umc.devine.admin.<도메인>.exception.code` | `AdminMemberSuccessCode` |

기존 도메인(`com.umc.devine.domain.*`)과 **분리된 `com.umc.devine.admin.*` 트리**를 사용합니다.

---

## AdminPrincipal 구조

관리자 컨트롤러에서 `@AuthenticationPrincipal AdminPrincipal`로 현재 관리자 정보를 받습니다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `clerkId` | String | 관리자 Clerk 사용자 ID (`user_xxx`). **판정 기준이자 로그 주체** |
| `email` | String | 이메일 (nullable — 토큰에 email 클레임이 없으면 null) |
| `name` | String | 이름 (nullable) |
| `imageUrl` | String | 프로필 이미지 URL (nullable) |
| `level` | `AdminLevel` | 관리자 권한 레벨 (현재 `ADMIN` 단일) |

```java
String clerkId = admin.getClerkId();
AdminLevel level = admin.getLevel();
```

> 관리자에 연결된 `Member` 정보가 필요하면 `memberRepository.findByClerkId(clerkId)`로 조회하세요. (관리자 계정과 일반 회원은 별개일 수 있으므로 존재하지 않을 수 있습니다.)

---

## 인증/인가 동작 방식 (내부)

```
요청 → Clerk JWT 검증(공통)
     ├─ /admin/v1/**  → [Admin Chain, @Order(1)]  ROLE_ADMIN 필요 → 아니면 403
     └─ 그 외          → [기존 Chain,  @Order(2)]  ROLE_USER
```

- **`AdminJwtAuthenticationConverter`**: JWT 검증 후 `AdminAuthorizationService.resolveAdmin(clerkId, email)`로 관리자 여부 판정 → 관리자면 `ROLE_ADMIN` 부여 + `AdminPrincipal` 세팅.
- **`AdminAuthorizationService`**: `clerk_id`로 `admin` 테이블 조회. 활성 관리자면 인가, 비활성(회수)이면 거절. 부트스트랩 대상이면 최초 접근 시 자동 등록(seeding).
- 관리자 기능 개발자는 이 클래스들을 **수정할 필요가 없습니다.** 컨트롤러만 추가하면 됩니다.

---

## 응답 / 에러 포맷

### 성공 응답

기존 `ApiResponse` + `BaseSuccessCode`를 그대로 사용합니다.

```java
public enum AdminMemberSuccessCode implements BaseSuccessCode {
    LIST_OK(HttpStatus.OK, "ADMIN_MEMBER200_1", "관리자 회원 목록을 조회했습니다.");
    // ...
}

return ApiResponse.onSuccess(AdminMemberSuccessCode.LIST_OK, result);
```

```json
{ "isSuccess": true, "code": "ADMIN_MEMBER200_1", "message": "...", "result": { ... } }
```

### 인증/인가 실패 (자동 처리)

| 상황 | 상태 | 응답 body |
|------|------|-----------|
| 토큰 없음/무효 | **401** | `{ "isSuccess": false, "code": "AUTH401_1", "message": "인증이 필요합니다." }` |
| 관리자 아님(ROLE_ADMIN 없음) | **403** | `{ "isSuccess": false, "code": "AUTH403_1", "message": "요청이 거부되었습니다." }` |

- 403 메시지는 **관리자 존재 여부를 노출하지 않는 일반 메시지**로 통일돼 있습니다(`AdminAccessDeniedHandler`).
- 이 두 경우는 프레임워크가 자동 처리하므로 컨트롤러에서 다룰 필요가 없습니다.

---

## 감사 로그 (자동)

`/admin/v1/**` 접근은 자동으로 구조화 로그에 기록됩니다. 별도 코드가 필요 없습니다.

- 인가된 접근: `[ADMIN-ACCESS] clerkId=user_xxx method=GET path=/admin/v1/members status=200`
- 거절된 접근(403): `[ADMIN-ACCESS-DENIED] clerkId=... method=... path=... status=403`

> 관리자가 **데이터를 변경하는 기능**(회원 정지/삭제, 결제 취소 등)을 추가할 때는, 되돌릴 수 없는 행위에 한해 **DB 감사 테이블** 도입을 검토하세요(현재는 구조화 로그만 사용).

---

## 관리자 계정 관리

### 관리자 등록 (부트스트랩)

관리자 계정 CRUD API는 아직 없습니다. 최초/추가 관리자는 **환경변수 부트스트랩**으로 등록합니다.

1. 대상자가 Clerk로 로그인 후 `/admin/v1/auth/me` 호출 → 403이 나면 서버 로그의 `[ADMIN-ACCESS-DENIED] clerkId=user_xxx`에서 `user_xxx` 확인 (또는 Clerk Dashboard → Users → User ID).
2. `.env`에 `ADMIN_BOOTSTRAP_CLERK_IDS=user_xxx,user_yyy` (콤마 구분) 설정 후 재시작.
3. 대상자가 다시 `/admin/v1/**` 접근 시 `admin` 테이블에 자동 등록되고 이후 정상 동작.

### 관리자 회수

`admin` 테이블의 해당 행 `is_active=false`로 변경(회수). **동시에 `ADMIN_BOOTSTRAP_CLERK_IDS`에서도 해당 clerk_id를 제거**하세요(회수 우선 정책상 목록에 남아도 재활성화되진 않지만, 목록 정리로 백도어를 최소화).

### 권한 레벨 확장

현재 `AdminLevel`은 `ADMIN` 단일입니다. 다단계가 필요하면 `AdminLevel` enum과 마이그레이션의 `admin_level_check` 제약을 **함께** 갱신하세요(둘 중 하나만 바꾸면 CHECK 위반 → 트랜잭션 abort).

---

## 테스트 작성법

관리자 컨트롤러 통합 테스트는 `ControllerIntegrationTestSupport`를 상속하고, `AdminPrincipal` + `ROLE_ADMIN` 인증을 주입합니다.

```java
class AdminMemberControllerTest extends ControllerIntegrationTestSupport {

    private Authentication adminAuth() {
        AdminPrincipal principal = AdminPrincipal.builder()
                .clerkId("admin_clerk_1").email("admin@devine.com")
                .name("관리자").level(AdminLevel.ADMIN)
                .build();
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void 관리자_목록_조회() throws Exception {
        mockMvc.perform(get("/admin/v1/members")
                        .with(authentication(adminAuth())))
                .andExpect(status().isOk());
    }

    @Test
    void 비관리자_403() throws Exception {
        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                new ClerkPrincipal("user_1", "u@d.com", "u", null), null, List.of());
        mockMvc.perform(get("/admin/v1/members").with(authentication(userAuth)))
                .andExpect(status().isForbidden());
    }
}
```

- `.with(authentication(...))`는 SecurityContext에 주체를 직접 주입하므로, 실제 Clerk 토큰 없이 인가 규칙을 검증할 수 있습니다.
- 참고 예시: `AdminAuthControllerTest`(관리자 200 / 비관리자 403 / 미인증 401).

---

## 자주 묻는 질문 (FAQ)

**Q. 새 관리자 API에 권한 체크 코드를 넣어야 하나요?**
아니요. `/admin/v1/**` 경로면 자동으로 `ROLE_ADMIN`이 강제됩니다.

**Q. `AdminPrincipal`이 null로 들어와요.**
경로가 `/admin/v1/**`인지 확인하세요. 그 외 경로는 일반 사용자 체인이 처리해 `ClerkPrincipal`이 주입됩니다.

**Q. 관리자용 공개(비인증) 엔드포인트가 필요해요.**
관리자 체인은 `/admin/v1/**` 전체에 `hasRole("ADMIN")`을 적용합니다. 공개가 꼭 필요하면 `ApiSecurityConfig`의 관리자 체인에 `permitAll` 매처를 추가해야 합니다(원칙적으로 지양).

**Q. 관리자의 이메일/이름이 null이에요.**
Clerk 기본 세션 토큰에는 email/name 클레임이 없습니다. 필요하면 Clerk 대시보드에서 세션 토큰에 클레임을 추가하세요. 단 **관리자 판정은 email과 무관**하므로(clerk_id 기준) 관리자 동작 자체엔 영향이 없습니다.

**Q. 관리자에 연결된 회원 정보가 필요해요.**
`memberRepository.findByClerkId(admin.getClerkId())`로 조회하세요(없을 수 있음).

---

## 참고 자료

- 일반 인증 연동: `docs/auth-integration-guide.md`
- 예시 구현: `com.umc.devine.admin.auth.*` (컨트롤러/서비스/시큐리티/DTO)
- 보안 설정: `com.umc.devine.global.config.ApiSecurityConfig` (`adminSecurityFilterChain`)
