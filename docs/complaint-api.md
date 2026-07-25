# 관리자 신고/제재 관리 API

`docs/moderation.md` 스펙 기반으로 구현된 관리자용 신고(Complaint) 조회·처리 API 문서입니다.
패키지: `com.umc.devine.admin.complaint` (devine-core: entity/repository/enums/exception, devine-api: dto/service/controller)

## 공통 사항

- **Base path**: `/admin/v1/complaints`
- **인증**: 관리자 인증/인가 기능이 아직 없어 이 3개 API는 인증을 강제하지 않습니다(`ApiSecurityConfig`에 permitAll 처리). 상태 변경 API만 로그인 세션이 있으면 처리자로 기록하고(`@CurrentMember(required = false)`), 없으면 처리자가 `null`로 남습니다.
  - TODO: 관리자 인증/인가가 추가되면 인증을 필수로 전환하고 관리자 권한 검증을 추가해야 합니다.
- **응답 포맷**: 공통 `ApiResponse` 봉투 사용

  ```json
  {
    "isSuccess": true,
    "code": "COMPLAINT200_1",
    "message": "성공적으로 신고 목록을 조회했습니다.",
    "result": { ... }
  }
  ```

- **에러 코드**

  | code | HTTP | 의미 |
  |---|---|---|
  | `COMPLAINT404_1` | 404 | 해당 신고를 찾을 수 없습니다. |
  | `COMPLAINT400_1` | 400 | 처리완료 시 세부 액션은 필수입니다. |
  | `COMPLAINT400_2` | 400 | 처리 사유는 필수입니다. |

- **성공 코드**: `COMPLAINT200_1`(목록 조회), `COMPLAINT200_2`(상세 조회), `COMPLAINT200_3`(상태 변경)

- **용어**
  - `complainant`(신고자): 신고를 접수한 유저
  - `respondentMember`(피신고자): 신고 대상이 된 유저
  - `resolver`(처리자): 상태를 변경한 관리자(세션에서 자동 추출, 현재는 로그인 유저면 누구나 가능)

---

## 1. 신고 목록 조회

`GET /admin/v1/complaints`

채팅/프로젝트/개발자 유형별, 상태별, 날짜별로 필터링해 신고 목록을 조회합니다. 접수 후 48시간이 지난 미처리 건은 `slaExceeded: true`로 표시됩니다.

### Query Parameters (`ComplaintReqDTO.SearchReq`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `targetType` | `CHAT` \| `PROJECT` \| `DEVELOPER` | X | 미지정 시 전체 조회 |
| `status` | `PENDING` \| `IN_REVIEW` \| `COMPLETED` | X | 미지정 시 전체 조회 |
| `fromDate` | `LocalDate` (yyyy-MM-dd) | X | 조회 시작일 (해당일 00:00:00부터) |
| `toDate` | `LocalDate` (yyyy-MM-dd) | X | 조회 종료일 (해당일 23:59:59까지) |
| `page` | Integer | X | 페이지 번호, 1부터 시작 (기본값 1) |
| `size` | Integer | X | 페이지 크기, 1~100 (기본값 10) |

### Response (`PagedResponse<ComplaintSummaryDTO>`)

```json
{
  "isSuccess": true,
  "code": "COMPLAINT200_1",
  "message": "성공적으로 신고 목록을 조회했습니다.",
  "result": {
    "content": [
      {
        "complaintId": 1,
        "targetType": "PROJECT",
        "complainantNickname": "complainant_1",
        "respondentNickname": "respondent_1",
        "createdAt": "2026-07-19T10:00:00",
        "status": "PENDING",
        "slaExceeded": true
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

- 조건에 해당하는 데이터가 없으면 에러가 아니라 `content: []`, `totalElements: 0`을 반환합니다.
- `slaExceeded`는 `status != COMPLETED && createdAt + 48시간 < 현재시각` 일 때 `true`입니다. `COMPLETED` 상태는 48시간이 지나도 항상 `false`입니다.

---

## 2. 신고 상세 조회

`GET /admin/v1/complaints/{complaintId}`

신고 사유, 신고자/피신고자 정보, 관련 콘텐츠 원문, 피신고자의 누적 신고/제재 이력을 함께 반환합니다.

### Path Variable
- `complaintId` (Long, required)

### Response (`ComplaintDetailRes`)

```json
{
  "isSuccess": true,
  "code": "COMPLAINT200_2",
  "message": "성공적으로 신고 상세를 조회했습니다.",
  "result": {
    "complaintId": 1,
    "targetType": "PROJECT",
    "targetId": 10,
    "complainantNickname": "complainant_1",
    "respondentNickname": "respondent_1",
    "reason": "프로젝트 게시글에 저작권 침해 소지가 있는 이미지를 사용했습니다.",
    "status": "PENDING",
    "action": null,
    "resolutionReason": null,
    "createdAt": "2026-07-19T10:00:00",
    "resolvedAt": null,
    "content": "프로젝트 원문 내용입니다.",
    "respondentComplaintCount": 2,
    "respondentHistory": [
      {
        "complaintId": 1,
        "targetType": "PROJECT",
        "complainantNickname": "complainant_1",
        "respondentNickname": "respondent_1",
        "createdAt": "2026-07-19T10:00:00",
        "status": "PENDING",
        "slaExceeded": false
      },
      {
        "complaintId": 3,
        "targetType": "CHAT",
        "complainantNickname": "complainant_2",
        "respondentNickname": "respondent_1",
        "createdAt": "2026-07-15T09:00:00",
        "status": "COMPLETED",
        "slaExceeded": false
      }
    ]
  }
}
```

- **`content` (관련 콘텐츠 원문)**: `targetType`에 따라 다르게 채워집니다.
  - `PROJECT`: 대상 프로젝트가 존재하고 삭제되지 않았으면 프로젝트 원문(`project_content`)을 그대로 반환. 프로젝트가 없거나 삭제된 상태(`ProjectStatus.DELETED`)면 `"삭제된 콘텐츠입니다"` 반환.
  - `CHAT`: `targetId`는 채팅방ID가 아니라 **신고당한 그 메시지 한 건의 `chat_message_id`**입니다. 해당 메시지를 DB에서 바로 조회해 `content`를 그대로 반환합니다(실시간 채팅 서비스 연동 없이 단건 조회). 메시지가 존재하지 않으면 `"삭제된 콘텐츠입니다"` 반환.
  - `DEVELOPER`: 신고 대상이 유저 자체라 관련 콘텐츠가 없으므로 `content: null`.
- **`respondentComplaintCount` / `respondentHistory`**: 피신고자가 받은 신고 전체 건수와 목록(최신순). "유저별 누적 신고/제재 이력 조회"는 별도 API가 아니라 이 필드로 통합 제공됩니다.
- 존재하지 않는 `complaintId`로 조회하면 `COMPLAINT404_1` (404)을 반환합니다.

---

## 3. 신고 처리 상태 변경

`PATCH /admin/v1/complaints/{complaintId}/status`

대기(`PENDING`) → 검토중(`IN_REVIEW`) → 처리완료(`COMPLETED`, 세부 액션: 경고/삭제/정지/기각)로 상태를 변경하고, 변경할 때마다 처리 이력을 기록합니다.

### Path Variable
- `complaintId` (Long, required)

### Request Body (`ComplaintReqDTO.UpdateStatusReq`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | `PENDING` \| `IN_REVIEW` \| `COMPLETED` | O | 변경할 상태 |
| `action` | `WARNING` \| `DELETE` \| `SUSPEND` \| `DISMISS` | `status`가 `COMPLETED`일 때만 필수 | 세부 액션 |
| `reason` | String | `status`가 `COMPLETED`일 때만 필수 | 처리 사유 |

### Response (`ComplaintResDTO.UpdateStatusRes`)

```json
{
  "isSuccess": true,
  "code": "COMPLAINT200_3",
  "message": "성공적으로 신고 처리 상태를 변경했습니다.",
  "result": {
    "complaintId": 1,
    "status": "COMPLETED",
    "action": "WARNING",
    "resolutionReason": "확인 결과 규정 위반",
    "resolvedAt": "2026-07-21T09:00:00",
    "reprocessWarning": false
  }
}
```

### 처리 로직
1. `complaintId` 존재 확인 → 없으면 `COMPLAINT404_1` (404)
2. `status`가 `COMPLETED`일 때 `action` 없으면 `COMPLAINT400_1`(400), `reason` 비어있으면 `COMPLAINT400_2`(400)
3. `action == SUSPEND`(계정 정지/정지해제/강제탈퇴 대상: 피신고자)는 **TODO 미구현** — 해당 하위 기능이 아직 없어 실제 호출 없이 신고 상태만 변경됩니다.
   `action == DELETE && targetType == PROJECT`(프로젝트 게시글 비노출 처리)는 구현되어 있어, 대상 프로젝트를 찾아 상태를 `DELETED`로 변경합니다(`Project.delete()`). 이미 삭제됐거나 존재하지 않는 프로젝트는 조치할 대상이 없으므로 조용히 넘어갑니다.
4. 신고의 `status`/`action`/`resolutionReason`/`resolver`/`resolvedAt` 업데이트
5. **`ComplaintHistory`에 이력 레코드 1건 추가** (상태가 실제로 바뀌었는지와 무관하게, 호출될 때마다 무조건 기록)
6. 이미 `COMPLETED` 상태인 신고를 다시 변경하는 경우, 응답의 `reprocessWarning: true`로 표시(프론트에서 확인 다이얼로그 노출용) — 예외는 아니며 정상 처리됨
7. 서비스 메서드 전체가 `@Transactional`로 묶여 있어, 프로젝트 비노출 처리(또는 향후 연동될 정지 처리)에서 예외가 발생하면 신고 상태 변경까지 자동으로 롤백됩니다.

---

## 미구현 영역 (TODO)

| 항목 | 상태 |
|---|---|
| 관리자 인증/인가 | 없음 — 3개 API 모두 인증 없이 호출 가능 |
| 계정 정지/정지해제/강제탈퇴 연동 (`action == SUSPEND`) | 미구현 — 신고 상태만 변경 |

## 참고

- 데이터베이스: `complaint`(신고), `complaint_history`(처리 이력) 테이블. 마이그레이션: `V20260721000000__297_add_complaint.sql`, `V20260721010000__297_add_complaint_history.sql`
- 로컬 수동 테스트용 시드 데이터: `docs/seed_complaint_test_data.sql`
- Swagger UI에서도 동일 스펙 확인 가능 (`Admin Complaint` 태그)
