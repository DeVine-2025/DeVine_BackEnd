# 관리자 프로젝트 노출 관리 API

신고 처리 결과 등에 따라 프로젝트 게시글을 유저 화면에서 노출/비노출로 전환하는 관리자 API 문서입니다.
패키지: `com.umc.devine.admin.project` (devine-api: dto/service/controller, devine-core: `Project` 엔티티 필드)

## 공통 사항

- **Base path**: `/admin/v1/projects`
- **인증**: 관리자 인증/인가 기능이 아직 없어 인증을 강제하지 않습니다(`ApiSecurityConfig`에 permitAll 처리). 로그인 세션이 있으면 처리자로 기록하고(`@CurrentMember(required = false)`), 없으면 처리자가 `null`로 남습니다.
  - TODO: 관리자 인증/인가가 추가되면 인증을 필수로 전환하고 관리자 권한 검증을 추가해야 합니다.
- **응답 포맷**: 공통 `ApiResponse` 봉투 사용
- **성공 코드**: `ADMIN_PROJECT200_1`(노출 상태 변경)
- **에러 코드**

  | code | HTTP | 의미 |
  |---|---|---|
  | `PROJECT404_1` | 404 | 존재하지 않는 프로젝트입니다. (이미 삭제된 프로젝트 포함) |

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

- 유저 화면의 모든 조회 경로에서 제외됩니다: 프로젝트 상세, 검색 목록, 주간 베스트, 추천, **소유자의 "내 프로젝트" 목록, 참여 중인 프로젝트 목록**.
- 소유자도 수정/삭제/상태변경이 불가능합니다(404). 원본이 보존돼야 관리자가 신고 건을 판단할 수 있기 때문입니다.
- 관리자 신고 상세 조회(`GET /admin/v1/complaints/{id}`)에서는 **비노출 프로젝트의 원문이 그대로 조회**됩니다.
- 첨부 이미지는 삭제되지 않습니다. 고아 이미지 정리 스케줄러는 `DELETED`만 실제 삭제로 취급하므로 비노출 프로젝트의 이미지는 보존됩니다.

---

## 프로젝트 노출/비노출 전환

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

## 신고 처리 연동

`PATCH /admin/v1/complaints/{complaintId}/status`에서 `action == DELETE && targetType == PROJECT`인 경우, 신고 서비스가 이 모듈의 `ProjectVisibilityCommandService.hideForModeration()`을 호출해 대상 프로젝트를 비노출 처리합니다.

- 비노출 처리가 실제로 수행되면 신고 레코드에 **연동 처리 완료**로 표시됩니다(`complaint.linked_action_completed = true`, `linked_action_at` 기록). 이 값은 신고 상세/상태변경 응답의 `linkedActionCompleted`로 노출됩니다.
- 대상 프로젝트가 존재하지 않거나 이미 삭제된 경우 조치할 대상이 없으므로 **예외 없이 넘어가고 연동 미완료(`false`)로 남습니다.** 신고 상태 변경 자체는 정상 완료됩니다.
- `hideForModeration()`이 관리자 API용 `changeVisibility()`와 달리 예외를 던지지 않는 이유: 두 서비스가 같은 트랜잭션에서 동작하므로, 예외가 서비스 밖으로 나가면 호출자가 catch하더라도 트랜잭션이 rollback-only로 마킹돼 신고 상태 변경까지 함께 롤백됩니다.

---

## 미구현 영역 (TODO)

| 항목 | 상태 |
|---|---|
| 관리자 인증/인가 | 없음 — 인증 없이 호출 가능 |
| 노출 상태 변경 이력(append-only) | 없음 — 마지막 처리자·처리시각만 `project` 테이블에 보관 |
| 소유자 알림 발송 | 없음 — 비노출 전환 시 프로젝트 소유자에게 별도 통지하지 않음 |

## 참고

- 마이그레이션: `V20260727000000__316_add_project_visibility.sql`(프로젝트 노출 컬럼), `V20260727010000__316_add_complaint_linked_action.sql`(신고 연동 표시 컬럼)
- 관련 문서: `docs/complaint-api.md`
- Swagger UI에서도 동일 스펙 확인 가능 (`Admin Project` 태그)
