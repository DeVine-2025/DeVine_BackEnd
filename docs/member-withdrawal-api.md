# 회원 탈퇴 API

이슈 #339 기반으로 구현된 회원 자진 탈퇴 API 문서입니다. 노션 "01-1 회원 탈퇴 개발 핸드오프"(PRD / 기능명세서 / 화면설계서)를 기준으로 합니다.
패키지: `com.umc.devine.domain.member` (controller/service/helper), 커밋 이후 외부 작업은 `com.umc.devine.infrastructure.outbox`

| 엔드포인트 | 설명 | 화면 |
|---|---|---|
| `GET /api/v1/members/me/withdrawal-preview` | 탈퇴 미리보기 | SCR-MEM-01~03 |
| `POST /api/v1/members/me/matchings/cancel-pending` | 대기 중인 지원/제안 일괄 취소 | SCR-MEM-01 |
| `POST /api/v1/members/me/withdrawal` | 탈퇴 실행 | SCR-MEM-04 |
| `PATCH /admin/v1/payments/refunds/{refundId}/complete` | 탈퇴 환불 신청 완료 처리(관리자) | - |

## 공통 사항

- **인증**: 로그인 필요. 정지(`SUSPENDED`), 강제탈퇴 예정(`PENDING_WITHDRAWAL`) 회원도 위 탈퇴 API 3개는 호출할 수 있습니다(약관 제7조). 다른 API는 지금처럼 막혀 있습니다.
- **탈퇴한 회원**: 탈퇴한 뒤 같은 토큰으로 API를 부르면 다른 미가입 회원과 같이 `403 AUTH403_1`이 내려갑니다.
- **응답 포맷**: 공통 `ApiResponse` 봉투 사용
- **화면 흐름**: 탈퇴 화면을 띄울 때마다 미리보기를 다시 호출합니다(다른 탭에서 상태가 바뀌었을 수 있음).

```
설정 > 회원 탈퇴
  └ GET withdrawal-preview
      ├ pendingCount > 0      → SCR-MEM-01 (POST cancel-pending 후 다시 미리보기)
      ├ SCR-MEM-02 안내 (generatingReport가 true면 생성 중 리포트 안내 문구 표시)
      ├ remainingCredits > 0  → SCR-MEM-03 (refundableCredits > 0일 때만 환불 선택지)
      └ SCR-MEM-04 확인 → POST withdrawal → 성공하면 Clerk signOut 후 SCR-MEM-05
```

---

## 1. 탈퇴 미리보기

`GET /api/v1/members/me/withdrawal-preview`

**성공 코드**: `MEMBER200_11`

### Response (`WithdrawalResDTO.PreviewDTO`)

| 필드 | 타입 | 설명 |
|---|---|---|
| `pendingMatchings` | List | 수락/거절 전인 지원/제안 목록(아래 표) |
| `pendingCount` | int | 대기 건 수. 1 이상이면 탈퇴할 수 없습니다 |
| `reportCount` | long | 지워질 리포트 수(완료된 리포트, 생성권 1개당 1개) |
| `generatingReport` | boolean | 생성 중인 리포트가 있는지. 탈퇴하면 결과가 사라지고 생성권은 돌아오지 않습니다 |
| `remainingCredits` | int | 남은 생성권 전체 |
| `refundableCredits` | int | 환불 가능 개수 |
| `nonRefundableCredits` | int | 환불 불가 개수(유효기간 만료 또는 무상 지급) |
| `refundAmount` | long | 환불 예정 금액(원) |
| `refundContactEmail` | String | 환불 연락 이메일 기본값(가입 이메일). 없으면 `null` |

`pendingMatchings` 항목(`PendingMatchingDTO`)

| 필드 | 타입 | 설명 |
|---|---|---|
| `matchingId` | Long | 매칭 ID |
| `projectName` | String | 프로젝트 이름 |
| `counterpartNickname` | String | 상대방 닉네임 |
| `matchingType` | `APPLY` / `PROPOSE` | 지원 / 제안 |
| `direction` | `SENT` / `RECEIVED` | 내가 보낸 건 / 받은 건 |

### 환불 계산 규칙

`remaining_count`가 숫자 하나뿐이라 결제 건별 남은 수량을 추정합니다(기능명세서 3장, 대표 결정 10-28).

- 무상분을 먼저 쓰고, 유료분은 먼저 산 것부터 쓴 것으로 봅니다. 그래서 남은 생성권은 최근 결제분부터 채웁니다.
- 결제일로부터 1년이 지난 결제분은 만료(환불 불가), 유료 결제분을 다 채우고 남는 개수는 무상분(환불 불가)입니다.
- 단가는 실제 결제 금액(쿠폰 할인 후) ÷ 지급 개수이고 1원 미만은 버립니다. 지급 개수 전부가 환불 가능하고 환불 이력이 없을 때만 결제 금액 전액을 돌려줍니다.
- 0원 결제(100% 쿠폰)는 무상분으로 봅니다. 관리자가 결제 전체를 환불한 건은 환불 대상에서 빠집니다.

계산은 `CreditRefundCalculator` 하나에 모아 두었습니다. 생성권 장부(04-2)가 생기면 이 부분만 바꾸면 됩니다.

---

## 2. 대기 중인 지원/제안 일괄 취소

`POST /api/v1/members/me/matchings/cancel-pending`

**성공 코드**: `MEMBER200_12`

내가 보냈거나 받은 대기 건(지원, 제안 모두)을 취소하고 상대방에게 알림을 보냅니다. 알림에는 탈퇴한다는 내용이 없습니다. 대기 건이 없으면 `canceledCount: 0`으로 성공합니다.

| 상황 | 상대방 알림 타입 | 알림 내용 |
|---|---|---|
| 지원 취소 | `MATCHING_APPLICATION_CANCELLED` (신규) | `'{프로젝트명}' 프로젝트 지원이 취소됐어요.` |
| 제안 취소 | `MATCHING_PROPOSAL_CANCELLED` | `'{프로젝트명}' 프로젝트 제안이 취소됐어요.` |

취소한 건이 걸린 프로젝트가 `DEADLINE_PASSED`이고 대기 건이 0이 되면 바로 `CLOSED`로 바뀝니다(#335 마감 확인 훅).

### Response (`WithdrawalResDTO.CancelPendingResultDTO`)

| 필드 | 타입 | 설명 |
|---|---|---|
| `canceledCount` | int | 취소한 건수 |

---

## 3. 탈퇴 실행

`POST /api/v1/members/me/withdrawal`

**성공 코드**: `MEMBER200_13`

### Request Body (`WithdrawalReqDTO.WithdrawReq`)

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `confirmed` | Boolean | O | `true`여야 합니다 |
| `refundRequested` | Boolean | X | 환불 신청 여부(기본값 `false`). 환불 가능 생성권이 없으면 무시됩니다 |
| `refundContactEmail` | String | 환불 신청 시 O | SCR-MEM-03에서 확인/수정한 환불 연락 이메일 |
| `reason` | String | X | 탈퇴 사유(아래 표). 회원과 연결하지 않고 통계로만 남깁니다 |

| reason | 화면 문구 |
|---|---|
| `NO_USEFUL_FEATURE` | 쓸 기능이 없어요 |
| `MATCHING_UNSATISFIED` | 원하는 매칭이 안 돼요 |
| `REPORT_UNSATISFIED` | 리포트가 기대와 달라요 |
| `USING_OTHER_SERVICE` | 다른 서비스를 써요 |
| `OTHER` | 기타 |

### Response (`WithdrawalResDTO.WithdrawResultDTO`)

| 필드 | 타입 | 설명 |
|---|---|---|
| `withdrawn` | boolean | 항상 `true` |
| `refundRequested` | boolean | 환불 신청이 접수됐는지. SCR-MEM-05의 환불 안내 문구 표시 여부에 씁니다 |

### 에러 코드

| code | HTTP | 의미 |
|---|---|---|
| `COMMON400_2` | 400 | `confirmed` 누락 또는 `false`, 이메일 형식 오류. `result`에 필드별 메시지 |
| `MEMBER400_6` | 400 | 환불을 신청했는데 환불 연락 이메일이 없음 |
| `AUTH403_1` | 403 | 가입되지 않았거나 이미 탈퇴한 회원 |
| `MEMBER409_3` | 409 | 대기 중인 지원/제안이 남아 있음. `result.pendingCount`에 건수가 담기고, 화면은 SCR-MEM-01로 돌아갑니다 |

```json
{
  "isSuccess": false,
  "code": "MEMBER409_3",
  "message": "진행 중인 지원이나 제안이 있어요. 정리한 뒤 탈퇴할 수 있어요.",
  "result": { "pendingCount": 2 }
}
```

### 탈퇴 후 프론트 처리

- 성공 응답을 받으면 Clerk `signOut()`을 호출해 로그아웃합니다. 서버는 커밋 뒤 Clerk 사용자를 지우지만 바로 지워지지 않을 수 있습니다(실패하면 재시도). 그 사이에도 서버 API는 `403`으로 막혀 있습니다.
- 같은 GitHub 계정으로 다시 가입하면 새 회원으로 가입되고 이전 데이터는 없습니다. 이전 닉네임도 다시 쓸 수 있습니다.

### 데이터 처리 (기능명세서 5장)

한 트랜잭션으로 처리하고, 중간에 실패하면 전부 롤백됩니다.

| 대상 | 처리 |
|---|---|
| 회원 행 | 익명화해서 남김. 닉네임은 `deleted#{회원ID}`, 이름/주소/소개/이미지/GitHub 계정/clerk_id는 비움, 상태 `DELETED` |
| 연락처, 약관 동의, 관심 도메인, 기술스택, 쿠폰, 받은 알림 | 삭제 |
| GitHub 데이터 | 레포, 리포트(공개 포함), 리포트 임베딩 삭제 |
| 내 프로젝트 | 프로젝트 삭제 함수(#335) 재사용. 매칭 이력이 있으면 이름만 남기고 `DELETED`, 없으면 행 삭제 |
| 북마크 | 내가 한 북마크, 남이 나를 북마크한 행 삭제 |
| 이미지 | 행 삭제, S3 객체는 커밋 뒤 삭제 |
| 채팅 | 내가 보낸 메시지만 삭제, 방은 상대방을 위해 남기고 내 쪽만 나간 상태 |
| 매칭 이력, 결제/환불 기록 | 보관(아래 파기 배치 참고) |
| 남은 생성권 | 환불 여부와 상관없이 0 |
| 상태 변경 이력 | 기존 행 삭제, 자진 탈퇴(`SELF_WITHDRAW`) 행 1개 추가 |
| 신고 기록 | 행은 남기고 신고자/피신고자/처리자 칸만 비움 |
| 상대가 받은 알림 | 행과 보낸 사람은 남기고 본문의 `{닉네임}님`을 `탈퇴한 회원님`으로 바꿈 |

커밋 뒤에는 아웃박스가 GitHub 권한 회수, Clerk 사용자 삭제, S3 이미지 삭제 순서로 실행합니다. 실패하면 1분부터 2배씩 늘려 재시도하고(최대 6시간 간격, 10회), 끝내 실패하면 운영자에게 메일을 보냅니다. 외부 정리가 실패해도 탈퇴는 성공으로 처리됩니다.

---

## 4. 탈퇴한 회원 표시 (기존 API 변경)

남은 기록에서 탈퇴한 회원은 닉네임 `"탈퇴한 회원"`, 이미지 `null`로 내려가고 `withdrawn` 필드가 추가됩니다. 프론트는 `withdrawn`이 `true`이면 프로필 링크를 막고 기본 이미지를 보여 줍니다.

| API / DTO | 추가 필드 |
|---|---|
| 채팅방 목록/상세 `ChatResDTO.OtherMemberInfo` | `withdrawn` |
| 채팅 메시지 `ChatResDTO.ChatMessageDetail` | `senderWithdrawn` |
| 알림 `NotificationResDTO.SenderInfo` | `withdrawn` |
| PM 지원 현황 `MatchingResDTO.DeveloperMatchingInfo` | `developerWithdrawn` |

- 채팅: 상대가 탈퇴했으면 입력창을 막고 "탈퇴한 회원과는 대화할 수 없어요"를 보여 줍니다. 메시지를 보내면 `CHAT400_4`가 내려갑니다.
- 공개 프로필, 공개 리포트 URL, 개발자 찾기, 추천에서는 탈퇴 회원이 보이지 않습니다(404 또는 목록 제외).
- 관리자 신고 화면: 칸이 비워진 신고자/피신고자는 `"탈퇴한 회원"`으로 보입니다.

---

## 5. 관리자: 탈퇴 환불 신청

탈퇴할 때 환불을 신청하면 결제 건마다 `payment_refund` 행이 `REQUESTED`, `source = WITHDRAWAL`로 생기고 운영자 메일(`[탈퇴 환불 신청] 결제 N건 / 총 ○○원`)이 갑니다.

- 운영자는 3영업일 안에 포트원 콘솔에서 `refundAmount`만 부분 취소합니다. 결제 전체를 취소하면 안 됩니다.
- 관리자 결제 조회의 상태에 `REFUND_REQUESTED`가 추가됐습니다.
- `REQUESTED` 행이 있는 결제는 기존 전액 환불 API(`POST /admin/v1/payments/{paymentId}/refund`)가 `409 PAYMENT409_1`로 막힙니다.

### 완료 처리

`PATCH /admin/v1/payments/refunds/{refundId}/complete` (관리자 인증 필요)

부분 취소를 마친 뒤 호출하면 `COMPLETED`로 바꾸고 환불 연락 이메일을 지웁니다. PG는 호출하지 않습니다.

**성공 코드**: `PAYMENT200_6`

| code | HTTP | 의미 |
|---|---|---|
| `PAYMENT404_2` | 404 | 환불 신청을 찾을 수 없음 |
| `PAYMENT409_2` | 409 | 접수(`REQUESTED`) 상태의 탈퇴 환불 신청이 아님 |

---

## 6. 정지/소명 중 탈퇴자 재가입 제한 (기본값 OFF)

정지 중이거나 강제탈퇴 예정인 회원이 탈퇴하면 가입 이메일 해시(HMAC-SHA256), 사유, 제재 요약을 1년 보관하고 같은 이메일의 재가입을 막습니다(결정 C). 개인정보처리방침 개정 후 대표 요청이 있을 때 켭니다.

- 플래그가 꺼져 있으면 일반 탈퇴와 같습니다.
- 가입 검사는 항상 켜져 있습니다. 보관 중인 이메일로 가입하면 `403 MEMBER403_3`("이용이 제한된 계정이에요. 문의: projectdevine2025@gmail.com")이 내려갑니다.
- 탈퇴 전에 Clerk에서 이메일을 받지 못하면 일반 탈퇴로 처리하고 운영자에게 알립니다. 탈퇴 자체는 막지 않습니다.

---

## 7. 보관 만료 파기 배치

`WithdrawalRetentionPurgeScheduler`, 매일 04:30(KST). 탈퇴일은 자진 탈퇴(`SELF_WITHDRAW`) 이력 시각입니다.

| 대상 | 처리 |
|---|---|
| 매칭 이력 | 한쪽이 탈퇴하고 1년이 지나면 그쪽이 쓴 내용만 비움(지원은 개발자, 제안은 PM). 양쪽 모두 탈퇴했으면 나중에 탈퇴한 쪽 기준 1년 뒤 행 삭제 |
| 이름만 남은 프로젝트 | `DELETED`이고 가리키는 매칭이 없으면 행 삭제 |
| 재가입 제한 정보 | 만료되면 삭제하고, 남겨 둔 제재 이력과 신고 식별 칸도 정리 |
| 결제/환불 기록 | 탈퇴 회원의 결제 중 결제일과 환불 완료일이 모두 5년 지나면 삭제 |

단계마다 따로 실행하고, 실패하면 운영자에게 메일을 보냅니다. 조건이 그대로라 다음 날 다시 처리됩니다.

---

## 환경 변수

모두 선택 사항입니다.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `WITHDRAWAL_OPERATOR_EMAIL` | `projectdevine2025@gmail.com` | 환불 신청, 배치 실패, 아웃박스 최종 실패 알림 수신 |
| `GITHUB_OAUTH_CLIENT_ID`, `GITHUB_OAUTH_CLIENT_SECRET` | 없음 | Clerk에 연결된 GitHub OAuth 앱 값. 없으면 GitHub 권한 회수를 건너뜀 |
| `WITHDRAWAL_RETAIN_SANCTIONED_MEMBER` | `false` | 정지/소명 중 탈퇴자 재가입 제한 플래그 |
| `WITHDRAWAL_EMAIL_HASH_PEPPER` | 없음 | 이메일 해시 비밀값. 플래그를 켜면 필수(없으면 앱 시작 실패). 한 번 정하면 바꾸지 않음 |
| `OUTBOX_POLL_INTERVAL`, `OUTBOX_MAX_ATTEMPTS` | `60000`, `10` | 아웃박스 재시도 주기(ms), 재시도 한도 |

## 범위 밖

- GitHub, Google 연동 해제 (다음 핸드오프)
- 관리자 "탈퇴 환불 신청 목록" 화면 (관리자페이지 Phase 3)
- 관리자 강제탈퇴 확정 시 개인정보 익명화 (별도 작업)
