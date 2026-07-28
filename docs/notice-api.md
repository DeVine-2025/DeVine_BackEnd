# 공지사항(Notice) API

이슈 #320 기반으로 구현된 공지사항 CRUD API 문서입니다.
패키지: 관리자용 `com.umc.devine.admin.notice`, 유저용 `com.umc.devine.domain.notice`
(devine-core: entity/enums/repository/exception, devine-api: dto/converter/service/controller/successCode)

## 공통 사항

- **Base path**: 관리자 `/admin/v1/notices`, 일반 유저 `/api/v1/notices`
- **인증**
  - 유저 조회 API 2개는 **비회원 허용**(`ApiSecurityConfig`에서 `GET /api/v1/notices`, `GET /api/v1/notices/*` permitAll).
  - 관리자 API는 관리자 인증/인가가 아직 없어 인증을 강제하지 않습니다(`ApiSecurityConfig`에 permitAll 처리).
    - TODO: 관리자 인증/인가(#294)가 머지되면 `/admin/v1/**`를 담당하는 admin SecurityFilterChain이 `ROLE_ADMIN`을 강제하므로, `ApiSecurityConfig`의 `/admin/v1/notices/**` permitAll 한 줄을 제거해야 합니다.
- **응답 포맷**: 공통 `ApiResponse` 봉투 사용

  ```json
  {
    "isSuccess": true,
    "code": "NOTICE200_1",
    "message": "공지사항 목록을 성공적으로 조회했습니다.",
    "result": { ... }
  }
  ```

- **에러 코드**

  | code | HTTP | 의미 |
  |---|---|---|
  | `NOTICE404_1` | 404 | 해당 공지사항을 찾을 수 없습니다. (유저 조회에서는 "게시 중이 아님"도 포함) |
  | `NOTICE400_1` | 400 | 게시 종료 일시는 시작 일시보다 뒤여야 합니다. |
  | `NOTICE400_2` | 400 | 제목과 내용은 비어 있을 수 없습니다. (수정 시 빈 문자열 전송) |
  | `COMMON400_2` | 400 | 필수값 검증 실패 (등록 시 제목/내용 미입력). `result`에 필드별 메시지가 담깁니다 |

- **성공 코드**: 관리자 `ADMINNOTICE201_1`(등록), `ADMINNOTICE200_1`(목록), `ADMINNOTICE200_2`(상세), `ADMINNOTICE200_3`(수정), `ADMINNOTICE200_4`(삭제) / 유저 `NOTICE200_1`(목록), `NOTICE200_2`(상세)

---

## 노출 규칙 (가장 중요)

공지의 노출 여부는 **스케줄러 없이 조회 시점에 계산**합니다. 저장되는 것은 아래 3개 값뿐입니다.

| 컬럼 | 의미 |
|---|---|
| `display_start_at` | 게시 시작 일시. `NULL`이면 시작 제한 없음 |
| `display_end_at` | 게시 종료 일시. `NULL`이면 종료 제한 없음 |
| `is_exposed` | 관리자의 수동 노출 스위치 |

유저에게 노출되는 조건은 다음 셋의 AND이며, **경계 시각은 포함**됩니다.

```
is_exposed = true
AND (display_start_at IS NULL OR display_start_at <= now)
AND (display_end_at   IS NULL OR display_end_at   >= now)
```

이 판정은 `Notice.isVisibleAt(now)`(엔티티)와 `NoticeRepository.findVisible`(JPQL) 두 곳에 있으며, 두 경로가 일치함을 `NoticeRepositoryTest`가 검증합니다. 목록은 페이징 정확성 때문에 DB에서 필터링합니다.

관리자 응답의 `displayStatus`는 **DB 컬럼이 아니라 조회 시점에 계산되는 파생값**입니다.

| displayStatus | 조건 |
|---|---|
| `HIDDEN` | `is_exposed = false` (기간과 무관) |
| `SCHEDULED` | 노출 대상이지만 `now < display_start_at` |
| `DISPLAYING` | 현재 노출 중 |
| `ENDED` | `now > display_end_at` |

---

# 관리자 API

## 1. 공지사항 등록

`POST /admin/v1/notices`

### Request Body (`AdminNoticeReqDTO.CreateNoticeReq`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | String | O | 공지 제목 (최대 100자) |
| `content` | String | O | 공지 본문 |
| `displayStartAt` | LocalDateTime | X | 게시 시작 일시. 미지정 시 시작 제한 없음 |
| `displayEndAt` | LocalDateTime | X | 게시 종료 일시. 미지정 시 종료 제한 없음 |
| `isExposed` | Boolean | X | 노출 여부. 미지정 시 `true` |

```json
{
  "title": "서비스 점검 안내",
  "content": "7월 30일 02:00~04:00 서비스 점검이 진행됩니다.",
  "displayStartAt": "2026-07-28T00:00:00",
  "displayEndAt": "2026-07-31T23:59:59",
  "isExposed": true
}
```

### Response (`AdminNoticeResDTO.NoticeDTO`)

```json
{
  "isSuccess": true,
  "code": "ADMINNOTICE201_1",
  "message": "공지사항이 성공적으로 등록되었습니다.",
  "result": {
    "noticeId": 1,
    "title": "서비스 점검 안내",
    "content": "7월 30일 02:00~04:00 서비스 점검이 진행됩니다.",
    "displayStartAt": "2026-07-28T00:00:00",
    "displayEndAt": "2026-07-31T23:59:59",
    "isExposed": true,
    "displayStatus": "DISPLAYING",
    "createdAt": "2026-07-28T13:00:00",
    "updatedAt": "2026-07-28T13:00:00"
  }
}
```

- 제목 또는 내용 미입력 시 **400** (`COMMON400_2`, `result`에 `{"title": "제목은 필수입니다."}` 형태의 필드별 메시지 포함)
- `displayStartAt >= displayEndAt`이면 **400** (`NOTICE400_1`)
- 성공 코드는 `ADMINNOTICE201_1`이지만, 프로젝트 전반이 `ApiResponse`를 그대로 반환하므로 **실제 HTTP 상태는 200**입니다(201은 body의 `code`에만 표기).

---

## 2. 공지사항 목록 조회

`GET /admin/v1/notices`

비노출·게시 예정·게시 종료 공지를 **모두 포함**해 최신순(`created_at DESC`)으로 페이징 조회합니다.

### Query Parameters (`PageRequest`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `page` | Integer | X | 페이지 번호, 1부터 시작 (기본값 1) |
| `size` | Integer | X | 페이지 크기 (기본값 10) |

### Response (`PagedResponse<NoticeDTO>`)

```json
{
  "isSuccess": true,
  "code": "ADMINNOTICE200_1",
  "message": "공지사항 목록을 성공적으로 조회했습니다.",
  "result": {
    "content": [
      {
        "noticeId": 2,
        "title": "게시 예정 공지",
        "content": "...",
        "displayStartAt": "2026-08-01T00:00:00",
        "displayEndAt": null,
        "isExposed": true,
        "displayStatus": "SCHEDULED",
        "createdAt": "2026-07-28T13:10:00",
        "updatedAt": null
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

---

## 3. 공지사항 상세 조회

`GET /admin/v1/notices/{noticeId}`

노출 여부와 무관하게 조회됩니다. 응답 형식은 등록 응답과 동일한 `NoticeDTO`입니다.
존재하지 않으면 `NOTICE404_1` (404).

---

## 4. 공지사항 수정

`PATCH /admin/v1/notices/{noticeId}`

### Request Body (`AdminNoticeReqDTO.UpdateNoticeReq`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | String | X | null이면 변경하지 않음 (최대 100자) |
| `content` | String | X | null이면 변경하지 않음 |
| `displayStartAt` | LocalDateTime | X | null이면 변경하지 않음 |
| `displayEndAt` | LocalDateTime | X | null이면 변경하지 않음 |
| `clearDisplayPeriod` | boolean | X | `true`면 게시 기간 두 값을 모두 제거해 상시 노출로 되돌림 (기본 `false`) |
| `isExposed` | Boolean | X | null이면 변경하지 않음 |

```json
{ "isExposed": false }
```

### 처리 로직

1. `noticeId` 존재 확인 → 없으면 `NOTICE404_1` (404)
2. `title`/`content`를 **빈 문자열/공백**으로 보내면 `NOTICE400_2` (400).
   `@NotBlank`는 null을 통과시켜 부분 수정과 구분되지 않으므로 서비스에서 별도로 검증합니다.
3. null이 아닌 필드만 반영. `clearDisplayPeriod=true`면 `displayStartAt`/`displayEndAt`을 모두 `null`로 설정(이때 개별 일시 값은 무시).
4. **변경이 적용된 최종 상태**로 게시 기간을 재검증 → 역전되면 `NOTICE400_1` (400)이며 트랜잭션이 롤백되어 아무것도 저장되지 않습니다.
   (한쪽 일시만 수정해도 기존 값과 조합되어 역전될 수 있기 때문입니다.)

응답은 수정 후의 `NoticeDTO` (`ADMINNOTICE200_3`).

---

## 5. 공지사항 삭제

`DELETE /admin/v1/notices/{noticeId}`

**Hard delete입니다 — 복구할 수 없습니다.** 일시적으로 감추려면 삭제 대신 `PATCH`로 `isExposed=false`를 사용하세요.

```json
{
  "isSuccess": true,
  "code": "ADMINNOTICE200_4",
  "message": "공지사항이 성공적으로 삭제되었습니다.",
  "result": null
}
```

존재하지 않으면 `NOTICE404_1` (404).

---

# 유저 API

## 6. 공지사항 목록 조회

`GET /api/v1/notices`

현재 게시 중인 공지만 최신순으로 페이징 조회합니다. 목록에는 **본문(`content`)이 포함되지 않습니다.**
쿼리 파라미터는 관리자 목록과 동일(`page`, `size`).

```json
{
  "isSuccess": true,
  "code": "NOTICE200_1",
  "message": "공지사항 목록을 성공적으로 조회했습니다.",
  "result": {
    "content": [
      { "noticeId": 1, "title": "서비스 점검 안내", "createdAt": "2026-07-28T13:00:00" }
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

- 게시 중인 공지가 없으면 에러가 아니라 `content: []`, `totalElements: 0`을 반환합니다.

---

## 7. 공지사항 상세 조회

`GET /api/v1/notices/{noticeId}`

```json
{
  "isSuccess": true,
  "code": "NOTICE200_2",
  "message": "공지사항을 성공적으로 조회했습니다.",
  "result": {
    "noticeId": 1,
    "title": "서비스 점검 안내",
    "content": "7월 30일 02:00~04:00 서비스 점검이 진행됩니다.",
    "createdAt": "2026-07-28T13:00:00"
  }
}
```

- **비노출이거나 게시 기간이 아닌 공지는 존재하더라도 `NOTICE404_1` (404)** 을 반환합니다. 공지의 존재 여부를 노출하지 않기 위함입니다.

---

## 미구현 영역 (TODO)

- **관리자 인증/인가**: `/admin/v1/notices/**`가 현재 permitAll입니다. #294 머지 후 permitAll 제거 및 컨트롤러 테스트의 `ROLE_ADMIN` 주입 전환이 필요합니다.
- **작성자 기록**: `created_by`/`updated_by`는 `BaseEntity`의 JPA Auditing으로 채워지며, 관리자 인증이 붙기 전까지는 관리자 주체가 기록되지 않습니다.
- **부가 속성**: 상단 고정(pinned), 카테고리, 조회수는 이번 범위에서 제외했습니다. 필요해지면 컬럼 추가 마이그레이션으로 확장하세요.
