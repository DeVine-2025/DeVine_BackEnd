# 관리자 프로젝트 노출 관리 API

관리자 페이지에서 프로젝트 게시글 목록을 조회하고, 신고 처리 결과 등에 따라 유저 화면에서 노출/비노출로 전환하는 관리자 API 문서입니다.
패키지: `com.umc.devine.admin.project` (devine-api: dto/converter/service/controller, devine-core: `Project` 엔티티 필드)

| 엔드포인트 | 설명 |
|---|---|
| `GET /admin/v1/projects` | 프로젝트 목록 조회 (ID/제목/작성자/등록일/노출상태) |
| `PATCH /admin/v1/projects/{projectId}/visibility` | 노출/비노출 전환 |

## 공통 사항

- **Base path**: `/admin/v1/projects`
- **인증**: 관리자 인증/인가 기능이 아직 없어 인증을 강제하지 않습니다(`ApiSecurityConfig`에 permitAll 처리). 로그인 세션이 있으면 처리자로 기록하고(`@CurrentMember(required = false)`), 없으면 처리자가 `null`로 남습니다.
  - TODO: 관리자 인증/인가가 추가되면 인증을 필수로 전환하고 관리자 권한 검증을 추가해야 합니다. (#294 관리자 로그인)
  - ⚠️ **목록 조회 API가 추가되면서 노출면이 커졌습니다.** 인증 없이 전체 프로젝트 목록 + 작성자 닉네임 + 비노출 여부를 열람할 수 있습니다. 기존 `/admin/v1/coupon/**`, `/admin/v1/complaints/**`와 동일한 미인증 상태이지만, 이 API는 **어떤 글이 제재로 비노출됐는지를 그대로 드러내므로** 인증 도입 우선순위가 더 높습니다.
- **응답 포맷**: 공통 `ApiResponse` 봉투 사용
- **성공 코드**: `ADMIN_PROJECT200_1`(노출 상태 변경), `ADMIN_PROJECT200_2`(목록 조회)
- **에러 코드**

  | code | HTTP | 의미 |
  |---|---|---|
  | `PROJECT404_1` | 404 | 존재하지 않는 프로젝트입니다. (이미 삭제된 프로젝트 포함) |

- **비노출 프로젝트에 대한 유저 API 에러 코드** (이 문서의 관리자 API가 아니라 `/api/v1/projects` 쪽에서 발생)

  | code | HTTP | 의미 |
  |---|---|---|
  | `PROJECT404_1` | 404 | 상세 조회/수정/삭제/상태변경 — 작성자 본인이 아닌 경우 존재하지 않는 것으로 처리 |
  | `MATCHING400_1` | 400 | 지원 시도 — 제재 사실을 노출하지 않기 위해 "모집 중인 프로젝트가 아닙니다"로 응답 |
  | `MATCHING400_9` | 400 | 제안 시도 — 요청자가 작성자 본인이므로 "비노출 처리된 프로젝트에는 제안할 수 없습니다"로 응답 |

---

## 노출 상태 모델

프로젝트의 **노출 여부**는 라이프사이클 상태(`project_status`)와 **분리된 별도 플래그**(`is_hidden`)로 관리됩니다.

| 컬럼 | 의미 |
|---|---|
| `project_status` | 라이프사이클 상태 (`RECRUITING` / `IN_PROGRESS` / `COMPLETED` / `DELETED`) |
| `is_hidden` | 유저 화면 노출 여부. `true`면 비노출 |
| `visibility_changed_by` | 노출 상태를 마지막으로 변경한 처리자 |
| `visibility_changed_at` | 노출 상태를 마지막으로 변경한 시각 |

두 값이 분리돼 있으므로 **비노출로 전환해도 원래 라이프사이클 상태가 그대로 보존되고, 다시 노출로 되돌리면 원래 상태로 복귀**합니다.

> 이전(#297)에는 `project_status = 'HIDDEN'`으로 덮어쓰는 방식이라 원래 상태가 유실돼 되돌릴 수 없었습니다. `HIDDEN` 상태값은 #316에서 제거됐습니다.

### 비노출 시 동작

**작성자 본인은 자기 글을 계속 확인할 수 있습니다.** 조회는 열어두되 글을 바꾸는 행위는 막는 것이 원칙입니다.

#### 조회 경로

| 경로 | 작성자 본인 | 그 외(타 회원·비로그인) |
|---|---|---|
| 프로젝트 상세 조회 `GET /api/v1/projects/{id}` | ✅ 조회 가능 (`visible: false`) | ❌ 404 |
| 내 프로젝트 목록 `GET /api/v1/projects/my/*` | ✅ 목록에 표시 (`visible: false`) | — |
| **공개 프로필 프로젝트 목록** `GET /api/v1/members/{nickname}/projects` | ❌ 제외 | ❌ 제외 |
| 검색 목록 / 주간 베스트 / 추천 | ❌ 제외 | ❌ 제외 |
| 참여 중인 프로젝트 목록(매칭 수락 개발자) | — | ❌ 제외 |
| 개발자 추천 필터용 목록 `GET /api/v1/projects/my/recruiting/created` | ❌ 제외 | — |

> ⚠️ **공개 프로필은 작성자 본인이 봐도 제외됩니다.** 이 엔드포인트는 뷰어가 아니라 프로필 주인을 기준으로 동작하는 비회원 허용 경로라, 본인 여부와 무관하게 제3자에게 보이는 것과 같은 결과를 냅니다. 작성자가 자기 비노출 글을 보려면 `GET /api/v1/projects/my/*` 또는 상세 조회를 사용해야 합니다.

#### 변경·행위 경로

| 경로 | 작성자 본인 | 그 외 |
|---|---|---|
| 수정 / 삭제 / 상태변경 | ❌ 404 | ❌ 404 |
| 프로젝트에 지원 `POST` (개발자) | — | ❌ `MATCHING400_1` |
| 개발자에게 제안 `POST` (PM) | ❌ `MATCHING400_9` | ❌ (소유자 아님) |
| **지원 취소 / 지원 파트 변경** | — | ✅ **허용** |

- **지원/제안은 차단됩니다.** 목록 UI에서 빠지는 것만으로는 부족합니다 — 프로젝트 ID를 직접 넣어 API를 호출하면 그대로 통과하므로 `MatchingValidator`에서 막습니다.
  - **지원(apply)은 `MATCHING400_1`("모집 중인 프로젝트가 아닙니다")로 응답합니다.** 호출자가 제3자라, 비노출 전용 에러를 주면 제재 사실이 노출되기 때문입니다.
  - **제안(propose)은 `MATCHING400_9`("비노출 처리된 프로젝트에는 제안할 수 없습니다")로 응답합니다.** 소유권 검증을 통과한 뒤라 요청자가 작성자 본인이고, 이미 `visible: false`를 통해 알고 있는 사실입니다.
- **지원 취소는 계속 허용됩니다.** 비노출 이전에 지원한 개발자가 취소조차 못 하고 갇히면 안 되기 때문입니다. 이 때문에 `MatchingCommandServiceImpl.getProject()`를 노출 기준 조회로 바꾸지 않고, 신규 참여 행위인 지원/제안만 validator에서 선별적으로 막습니다.

#### 그 밖의 동작

- **`visible` 필드**: 상세 조회(`UpdateProjectRes`)와 내 프로젝트 목록(`MyProjectInfo`) 응답에 포함됩니다. 작성자에게 "관리자가 비노출 처리한 글" 배지를 띄우는 데 사용하세요.
- **수정/삭제/상태변경은 계속 차단**됩니다(404). 원본이 보존돼야 관리자가 신고 건을 판단할 수 있기 때문입니다.
- **개발자 추천 필터용 목록에서만 제외**되는 이유: 이 목록은 "확인"이 아니라 개발자를 모집할 대상을 고르는 **액션 목록**입니다. 제재로 숨겨진 글로 개발자를 모집하게 두면 제재를 우회하게 됩니다.
- **참여 개발자에게는 보이지 않습니다.** 제재 사실이 제3자에게 노출되지 않도록 작성자 본인으로 한정했습니다.
- **조회수는 오르지 않습니다.** 비노출 프로젝트를 작성자가 열어봐도 `incrementViewCount`를 호출하지 않습니다.
- 관리자 신고 상세 조회(`GET /admin/v1/complaints/{id}`)에서는 **비노출 프로젝트의 원문이 그대로 조회**됩니다.
- 첨부 이미지는 삭제되지 않습니다. 고아 이미지 정리 스케줄러는 `DELETED`만 실제 삭제로 취급하므로 비노출 프로젝트의 이미지는 보존됩니다.

#### 구현 시 주의 (백엔드)

비노출 제외는 **리포지토리 기본값이 안전한 쪽**입니다. 새 조회 경로를 만들 때 아래를 지켜야 합니다.

| 메서드 | 비노출 | 용도 |
|---|---|---|
| `findAllByMemberAndStatusIn` / `findByMemberAndStatusIn` | 제외 | 기본값. 제3자에게 보일 수 있는 모든 경로 |
| `findAllByMemberAndStatusInIncludingHidden` | 포함 | **작성자 본인 목록 전용** |
| `ProjectQueryService.getMyProjects` | 포함 | 작성자 본인의 "내 프로젝트" |
| `ProjectQueryService.getPublicProjectsOf` | 제외 | 공개 프로필 등 제3자 경로 |

`getMyProjects`라는 이름은 "내" 프로젝트처럼 읽히지만 인자로 받은 회원의 목록을 돌려줄 뿐입니다. 공개 프로필처럼 **프로필 주인을 인자로 넘기는 경로에서 이 메서드를 쓰면 비노출 프로젝트가 제3자에게 새어나갑니다.** 그런 경로에는 반드시 `getPublicProjectsOf`를 사용하세요.

---

## 1. 관리자 프로젝트 목록 조회

`GET /admin/v1/projects`

관리자 페이지 테이블에 필요한 항목만 등록일 최신순으로 반환합니다.

### Query Parameters (`AdminProjectReqDTO.SearchReq`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `visible` | Boolean | X | 노출 상태 필터. `true`=노출 중만, `false`=비노출만. 미지정 시 전체 조회 |
| `page` | Integer | X | 페이지 번호, 1부터 시작 (기본값 1) |
| `size` | Integer | X | 페이지 크기, 1~100 (기본값 10) |

### Response (`PagedResponse<ProjectSummaryDTO>`)

```json
{
  "isSuccess": true,
  "code": "ADMIN_PROJECT200_2",
  "message": "성공적으로 프로젝트 목록을 조회했습니다.",
  "result": {
    "content": [
      {
        "projectId": 10,
        "title": "쇼핑몰 프로젝트",
        "authorNickname": "devine_pm",
        "createdAt": "2026-07-20T14:30:00",
        "visible": true
      }
    ],
    "page": 1,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true
  }
}
```

| 필드 | 설명 |
|---|---|
| `projectId` | 프로젝트 ID (키값). 노출 전환 API의 path variable로 그대로 사용 |
| `title` | 프로젝트 제목 |
| `authorNickname` | 글 작성자 닉네임 |
| `createdAt` | 등록일 |
| `visible` | 노출 상태 (`true`=노출, `false`=비노출) |

- **삭제된(`DELETED`) 프로젝트는 제외됩니다.** 삭제된 프로젝트는 노출 전환 API가 404를 반환하므로, 목록에 뜨는 모든 행이 곧 노출 전환 가능한 행입니다. 프론트에서 비활성 분기를 따로 둘 필요가 없습니다.
- 조건에 해당하는 데이터가 없으면 에러가 아니라 `content: []`, `totalElements: 0`을 반환합니다.
- 정렬은 등록일 최신순 고정입니다.

---

## 2. 프로젝트 노출/비노출 전환

`PATCH /admin/v1/projects/{projectId}/visibility`

### Path Variable
- `projectId` (Long, required)

### Request Body (`AdminProjectReqDTO.UpdateVisibilityReq`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `visible` | Boolean | O | 변경할 노출 상태 (`true`=노출, `false`=비노출) |

```json
{
  "visible": false
}
```

### Response (`AdminProjectResDTO.UpdateVisibilityRes`)

```json
{
  "isSuccess": true,
  "code": "ADMIN_PROJECT200_1",
  "message": "프로젝트 노출 상태가 변경되었습니다.",
  "result": {
    "projectId": 10,
    "visible": false,
    "changed": true,
    "processorMemberId": 3,
    "changedAt": "2026-07-27T10:00:00"
  }
}
```

| 필드 | 설명 |
|---|---|
| `visible` | 변경된 노출 상태 |
| `changed` | 노출 상태가 실제로 바뀌었으면 `true`. 이미 동일한 상태였으면 `false` |
| `processorMemberId` | 처리자 회원 ID. 로그인 세션이 없으면 `null` |
| `changedAt` | 처리 시각 |

### 처리 로직

1. `projectId` 존재 확인 → 없거나 이미 `DELETED` 상태면 `PROJECT404_1` (404)
2. 노출 상태(`is_hidden`) 업데이트, 처리자(`visibility_changed_by`)·처리시각(`visibility_changed_at`) 기록
3. **멱등성 보장**: 이미 동일한 노출 상태여도 예외 없이 정상 처리(200)됩니다. 이 경우 `changed: false`로 구분하며, 처리자·처리시각은 갱신됩니다(동일 상태 재요청도 "관리자가 그 상태를 확정한 처리"로 보기 때문).

---

## 3. 신고 처리 연동

`PATCH /admin/v1/complaints/{complaintId}/status`에서 `action == DELETE && targetType == PROJECT`인 경우, 신고 서비스가 이 모듈의 `ProjectVisibilityCommandService.hideForModeration()`을 호출해 대상 프로젝트를 비노출 처리합니다.

- 비노출 처리가 실제로 수행되면 신고 레코드에 **연동 처리 완료**로 표시됩니다(`complaint.linked_action_completed = true`, `linked_action_at` 기록). 이 값은 신고 상세/상태변경 응답의 `linkedActionCompleted`로 노출됩니다.
  - 이 필드는 **실행 이력**이지 현재 상태가 아닙니다. 이 API로 다시 노출시켜도 `true`로 남습니다. 현재 노출 상태는 프로젝트의 `is_hidden`으로 판단하세요.
- **재처리해도 비노출은 자동 해제되지 않습니다.** `DELETE`로 비노출된 신고를 `DISMISS` 등 다른 액션으로 재처리해도 프로젝트는 비노출로 유지되며, 복구는 이 API로 관리자가 명시적으로 수행해야 합니다. 어떤 신고가 비노출을 유발했는지 추적하지 않아 자동 해제 시 다른 신고의 제재까지 풀릴 수 있기 때문입니다.
- 대상 프로젝트가 존재하지 않거나 이미 삭제된 경우 조치할 대상이 없으므로 **예외 없이 넘어가고 연동 미완료(`false`)로 남습니다.** 신고 상태 변경 자체는 정상 완료됩니다.
- `hideForModeration()`이 관리자 API용 `changeVisibility()`와 달리 예외를 던지지 않는 이유: 두 서비스가 같은 트랜잭션에서 동작하므로, 예외가 서비스 밖으로 나가면 호출자가 catch하더라도 트랜잭션이 rollback-only로 마킹돼 신고 상태 변경까지 함께 롤백됩니다.

---

## 미구현 영역 (TODO)

| 항목 | 상태 |
|---|---|
| 관리자 인증/인가 | 없음 — 인증 없이 호출 가능. 목록 조회가 제재 대상 글을 그대로 드러내므로 #294 도입 시 최우선 적용 대상 |
| 노출 상태 변경 이력(append-only) | 없음 — 마지막 처리자·처리시각만 `project` 테이블에 보관 |
| 비노출 원인 신고 추적 (참조 카운팅) | 없음 — 신고 재처리 시 비노출 자동 해제를 구현하려면 이것이 선행돼야 합니다 |
| 소유자 알림 발송 | 없음 — 비노출 전환 시 프로젝트 소유자에게 별도 통지하지 않음. 소유자는 자기 글의 `visible: false`를 보고 인지해야 함 |
| 비노출 사유 노출 | 없음 — 어떤 신고로 비노출됐는지 추적하지 않아, 소유자에게 사유를 알려줄 수 없음 (참조 카운팅 도입이 선행돼야 함) |

## 참고

- 마이그레이션: `V20260727000000__316_add_project_visibility.sql`(프로젝트 노출 컬럼), `V20260727010000__316_add_complaint_linked_action.sql`(신고 연동 표시 컬럼)
- 관련 문서: `docs/complaint-api.md`
- Swagger UI에서도 동일 스펙 확인 가능 (`Admin Project` 태그)
