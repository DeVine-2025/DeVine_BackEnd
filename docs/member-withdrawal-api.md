# 회원 자진 탈퇴 및 관련 배치 문서

회원 자진 탈퇴 API, 관리자 환불 신청 API, 그리고 탈퇴 라이프사이클을 뒷받침하는 배치들의 문서입니다.

패키지:
- `com.umc.devine.domain.member.service.command.MemberWithdrawalCommandService` (devine-api): 자진 탈퇴 오케스트레이션
- `com.umc.devine.admin.ticket` (devine-api): 환불 신청 관리자 API
- `com.umc.devine.domain.ticket.entity.CreditRefundRequest` (devine-core): 환불 신청 엔티티
- `com.umc.devine.global.scheduler`, `com.umc.devine.global.scheduler.harddelete` (devine-api): 탈퇴 관련 배치 전체

---

## 1. 회원 자진 탈퇴 API

### 1-1. 탈퇴 안내 미리보기

`GET /api/v1/members/me/withdrawal/preview`

탈퇴 확인 화면에서 사용합니다. 잔여 리포트 생성권, 보유 쿠폰 수, 데이터 처리 범위 안내 표를 반환합니다.

**성공 코드**: `MemberSuccessCode.FOUND_WITHDRAWAL_PREVIEW`

응답(`MemberResDTO.WithdrawalPreviewDTO`):

| 필드 | 설명 |
|---|---|
| `remainingReportCredits` | 잔여 리포트 생성권 수 |
| `couponCount` | 보유 쿠폰 수(사용 여부 무관) |
| `dataScope` | 탈퇴 시 데이터 처리 범위 안내 목록(아래 3장 표와 동일한 내용) |

### 1-2. 회원 자진 탈퇴

`POST /api/v1/members/me/withdrawal`

요청(`MemberReqDTO.SelfWithdrawReq`):

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `confirmationText` | String | O | 본인확인 문구. 현재 닉네임을 그대로 재입력해야 합니다 |
| `refundRequested` | Boolean | X | 잔여 리포트 생성권 환불 신청 여부(기본값 `false`) |

**본인확인 방식이 비밀번호가 아니라 닉네임 재입력인 이유**: Clerk 기반 인증이라 비밀번호 필드 자체가 없습니다. 신원 확인이 아니라 오탈퇴 방지용 의도 확인이라, trim만 적용하고 대소문자는 그대로 비교합니다.

**에러 코드**

| code | HTTP | 의미 |
|---|---|---|
| `MEMBER400_6` | 400 | 확인 문구가 일치하지 않습니다. |
| `MEMBER400_7` | 400 | 현재 계정 상태(`SUSPENDED`, `PENDING_WITHDRAWAL`, `DELETED`)에서는 탈퇴할 수 없습니다. `ACTIVE`, `INACTIVE`만 허용합니다. |

**성공 코드**: `MemberSuccessCode.WITHDRAWN`

응답(`MemberResDTO.WithdrawalResultDTO`): `withdrawn`, `refundRequestCreated`, `creditsForfeitedOrRefunded`, `withdrawnAt`(실제 저장된 `Member.deletedAt`을 그대로 반환합니다. 별도로 `now()`를 다시 호출하지 않습니다).

### 처리 순서(하나의 트랜잭션)

1. 행 잠금(`findByIdForUpdate`, `PESSIMISTIC_WRITE`). 중복 클릭 등 동시 탈퇴 요청을 방지합니다.
2. 탈퇴 가능 상태 검증(`ACTIVE`, `INACTIVE`만 허용)
3. 본인확인 문구 검증
4. 잔여 리포트 생성권 처리. `refundRequested=true`이고 잔여 수량이 0보다 크면 `CreditRefundRequest`를 생성(`REQUESTED` 상태)한 뒤 생성권을 소멸(`voidCredits()`)합니다. 환불 미신청 시에는 소멸만 합니다.
5. 쿠폰 전량 삭제(`MemberCouponRepository.bulkDeleteByMember`, 사용/미사용 무관)
6. 본인이 신고자(complainant)인 신고 이력만 삭제합니다. 피신고자였던 이력은 보존합니다(다른 신고자의 기록 보호).
7. `Member.selfWithdraw()`. PII 즉시 익명화(닉네임, 이름, 주소, 이미지, 자기소개, GitHub 계정, clerkId), `deletedAt` 기록.
8. GitHub 원본 연동 데이터 즉시 삭제(`MemberGithubDataCleanupService`, 4장 참고)
9. 감사 로그 기록(`MemberStatusHistory`, action은 `SELF_WITHDRAW`)
10. `MemberWithdrawnEvent` 발행. 커밋 후 비동기로 Clerk 계정 삭제(`ClerkApiClient.deleteUser`, 404는 성공으로 흡수)
11. 탈퇴 완료 이메일 발송

**자진탈퇴는 이메일 해시 블랙리스트 대상이 아닙니다.** 블랙리스트는 강제탈퇴(자격상실) 전용입니다. 5장을 참고하세요.

---

## 2. 관리자 환불 신청 API

`GET/PATCH /admin/v1/ticket/refunds`. 자진 탈퇴 시 접수된 잔여 리포트 생성권 환불 신청을 관리자가 조회, 처리합니다.

패키지가 `admin.ticket`인 이유는 `CreditRefundRequest`, `MemberReportCredit`가 실제로 devine-core의 `domain.ticket` 패키지에 속해 있어, 관리자 API도 그 도메인 이름을 그대로 따랐기 때문입니다. 참고로 별도 기능인 `#302`의 결제 전액환불 API는 `admin.payment`, `/admin/v1/payments`로 완전히 분리되어 있습니다. 그쪽은 완료된 결제 건의 PG 취소, 이 문서가 다루는 건 탈퇴 시 미사용 생성권 환불 신청이라 서로 다른 개념입니다.

### 2-1. 환불 신청 목록 조회

`GET /admin/v1/ticket/refunds`

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | `REQUESTED`, `PROCESSED`, `EXPIRED` | X | 처리 상태 필터. 미지정 시 전체 조회 |
| `page`, `size` | Integer | X | 페이지네이션(1부터 시작, 기본 10) |

응답 항목(`RefundRequestDTO`): `refundRequestId`, `memberNickname`(회원이 하드삭제되어 `member_id`가 끊긴 행은 `"(하드삭제된 회원)"`으로 표시합니다. 6장 참고), `creditAmountAtRequest`, `status`, `requestedAt`, `processedAt`

### 2-2. 환불 신청 처리완료

`PATCH /admin/v1/ticket/refunds/{refundRequestId}`

동시 처리 요청이 경쟁하지 않도록 행 잠금(`findByIdForUpdate`, `PESSIMISTIC_WRITE`)을 적용합니다.

**에러 코드**

| code | HTTP | 의미 |
|---|---|---|
| `ADMINTICKET404_1` | 404 | 해당 환불 신청을 찾을 수 없습니다. |
| `ADMINTICKET400_1` | 400 | 이미 처리완료된 환불 신청입니다. |

**성공 코드**: `ADMINTICKET200_1`(목록 조회), `ADMINTICKET200_2`(처리완료)

**인증**: 관리자 인증(`ApiSecurityConfig`의 `/admin/**` → `ROLE_ADMIN`)으로 다른 관리자 API와 동일하게 커버됩니다.

### `CreditRefundStatus` 상태 전이

- `REQUESTED`에서 관리자가 처리완료하면 `PROCESSED`로 전이합니다.
- `REQUESTED`에서 하드삭제 유예기간이 만료되도록 관리자가 처리하지 않으면 `EXPIRED`로 전이합니다.

`EXPIRED`는 관리자가 처리하지 않아 환불 청구권이 소멸됐다는 감사 기록입니다. 자세한 내용은 6장을 참고하세요.

---

## 3. 탈퇴 시 데이터 처리 범위

| 데이터 항목 | 처리 |
|---|---|
| 회원 프로필 | 즉시 삭제(익명화) |
| GitHub 원본 연동 데이터 | 즉시 삭제 |
| GitHub 익명화 및 벡터화 데이터 | 보관 유지 |
| 채팅 메시지 | 발신자 익명 처리(상대방 대화 내용은 유지). `Member` PII가 익명화되므로 채팅 메시지 테이블 자체는 건드리지 않아도 자동으로 충족됩니다 |
| 매칭 지원/제안 이력 | 탈퇴 후 1년 보관 후 파기(`MatchingHistoryPurgeScheduler`) |
| 신고 및 제재 이력(본인이 신고한 건) | 즉시 삭제 |
| 신고 및 제재 이력(본인이 신고당한 건) | 보존(다른 신고자의 기록 보호) |
| 결제 및 리포트 생성권 구매 내역 | 5년 보관(하드삭제 대상에서 의도적으로 제외. 7장 참고) |
| 미사용 리포트 생성권 | 환불 신청 시 환불, 미신청 시 소멸 |
| 미사용 쿠폰 | 즉시 소멸(환불 대상 아님) |
| 로그인 이력 및 접속 IP | 3개월 후 자동 파기(`MemberLoginHistoryPurgeScheduler`, 탈퇴 여부 무관 전 회원 대상. 통신비밀보호법에 근거) |

---

## 4. 강제탈퇴 최종 확정 스케줄러

`MemberWithdrawalFinalizeScheduler`는 매일 새벽 4시, 관리자가 강제탈퇴(자격상실) 처리한 뒤 30일 소명 절차가 만료된 계정을 최종 확정합니다.

- 대상 회원별로 독립된 물리 트랜잭션(`TransactionTemplate`과 `PROPAGATION_REQUIRES_NEW`)에서 처리합니다. 특정 회원 처리 중 예외가 나도 그날 배치의 다른 회원들까지 함께 롤백되지 않고, 실패한 회원만 다음 배치에서 재시도됩니다(`MemberHardDeleteScheduler`와 동일한 설계 이유입니다).
- 처리 순서는 다음과 같습니다. 이메일 해시를 블랙리스트에 적재하고, `Member.finalizeWithdrawal()`(PII 즉시 익명화, `deletedAt` 기록)을 호출하고, 감사 로그(`WITHDRAWAL_FINALIZED`)를 남기고, `saveAndFlush`한 뒤 GitHub 연동 데이터를 삭제합니다.
- GitHub 연동 데이터 삭제는 자진 탈퇴(1장)와 완전히 동일한 로직을 `MemberGithubDataCleanupService`로 공유합니다.

---

## 5. 강제탈퇴자 재가입 제한 이메일 해시 블랙리스트

이용약관 제5조 3항에 따라, 강제탈퇴 확정 시점에만(자진탈퇴는 대상 아님) 이메일 해시를 1년간 보관하고 회원가입 시 대조해서 재가입을 막습니다.

- 해시는 서버 비밀키(pepper, `member.email-hash.secret`)를 사용한 HMAC-SHA256으로 계산합니다. DB가 유출되어도 원본 이메일 역산이 어렵도록 순수 SHA-256 대신 이 방식을 택했습니다.
- pepper가 설정되지 않으면 `EmailHasher` 빈 생성 시점(앱 기동 시)에 즉시 실패합니다(fail-fast).
- 회원가입(`MemberCommandServiceImpl.signup`)에서 이메일 해시가 블랙리스트에 활성 상태로 존재하면 가입을 거부합니다(`FORMER_MEMBER_BLOCKED`).

---

## 6. 회원 Hard Delete 배치

`MemberHardDeleteScheduler`는 매일 새벽 5시, 탈퇴(자진/강제 무관) 후 유예기간(기본 30일, `member.hard-delete.grace-period-days`)이 지난 계정의 개인정보 관련 데이터를 완전 삭제합니다. `member.hard-delete.enabled` 설정으로 기본 비활성화되어 있고, 운영 환경에서 준비되면 명시적으로 켭니다.

### 설계: `MemberHardDeleteHandler` 전략 패턴

이 스케줄러는 실제 정리 작업을 하나도 알지 못합니다. 대신 `MemberHardDeleteHandler` 인터페이스를 구현한 `@Component` 핸들러들을 `@Order` 순서대로 실행만 합니다.

```java
public interface MemberHardDeleteHandler {
    void handle(Member member);
}
```

새 회원 연관 테이블이 생기면, 이 인터페이스를 구현한 핸들러 클래스 하나를 추가하는 것으로 끝납니다. 스케줄러 자체는 수정할 필요가 없습니다. 각 핸들러는 그냥 지우는 것이 정답인지 스스로 판단할 책임을 집니다.

| 순서 | 핸들러 | 대상 | 방식 |
|---|---|---|---|
| 10 | `GithubDataHardDeleteHandler` | report_embedding, dev_report, git_repo_url, dev_techstack, contact | 삭제(`MemberGithubDataCleanupService` 재사용) |
| 20 | `MemberCouponHardDeleteHandler` | member_coupon | 삭제 |
| 30 | `OwnComplaintsHardDeleteHandler` | complaint(본인이 신고자인 것만), complaint_history | 삭제 |
| 40 | `MemberStatusHistoryHardDeleteHandler` | member_status_history | 대상 회원(subject) 기준 삭제, 처리자(processor) 기준 참조 해제 |
| 50 | `CreditRefundRequestHardDeleteHandler` | credit_refund_request | 삭제하지 않습니다. 미처리(REQUESTED) 건은 EXPIRED로 전이 후 member_id만 해제, 처리완료 건은 상태를 유지한 채 member_id만 해제합니다(아래 "왜 삭제하지 않는가" 참고) |
| 60 | `MemberReportCreditHardDeleteHandler` | member_report_credit | 삭제 |
| 70 | `MemberAgreementHardDeleteHandler` | member_agreement | 삭제 |
| 80 | `MemberLoginHistoryHardDeleteHandler` | member_login_history | 삭제 |
| 90 | `BookmarkHardDeleteHandler` | bookmark | 삭제 |
| 100 | `ImageHardDeleteHandler` | image | 업로더 참조만 해제합니다(자산은 다른 곳에서 계속 참조될 수 있습니다) |
| 110 | `NotificationHardDeleteHandler` | notification | 내가 받은 알림은 삭제, 내가 보낸 알림은 발신자 참조만 해제 |

마지막으로 `memberRepository.delete(member)`를 호출합니다. payment, matching, project, chat 등 다른 회원과 얽힌 레코드가 여전히 남아있으면 FK 위반으로 실패하고, 해당 회원만 건너뛰어 다음 배치에서 재시도합니다. 회원별로 독립된 물리 트랜잭션(`REQUIRES_NEW`)에서 처리되므로, 한 회원의 실패가 다른 회원의 삭제를 막지 않습니다.

### 왜 `credit_refund_request`는 삭제하지 않는가

이 테이블은 금전 청구 기록입니다. 관리자가 미처리 상태(`REQUESTED`)로 30일을 넘긴 뒤 그냥 삭제해버리면, 환불을 요청했는데 아무 기록도 없이 사라진 상태가 됩니다. 그래서 다음과 같이 처리합니다.

1. `CreditRefundStatus.EXPIRED`로 전이(처리일시도 기록)해 관리자 미처리로 소멸했다는 감사 기록을 남깁니다.
2. 행은 삭제하지 않고 `member_id`만 `null`로 끊습니다(마이그레이션으로 `member_id`를 nullable로 변경했습니다). 이미 `PROCESSED`인 건도 회원 하드삭제를 막지 않도록 `member_id`만 함께 해제합니다.
3. `processor_member_id`(환불을 처리한 관리자)도 별도로 참조가 끊깁니다. 관리자 계정도 `Member`이므로, 그 관리자가 나중에 탈퇴 후 하드삭제 대상이 되어도 자신이 처리자로 남은 다른 회원의 환불 기록 때문에 막히지 않습니다.
4. 관리자 API 응답(`AdminTicketConverter`)은 `member_id`가 끊긴 행에 대해 닉네임 대신 `"(하드삭제된 회원)"`을 표시합니다.

`member_status_history.processor_member_id`도 동일한 이유로 별도 참조 해제 대상입니다(40번 핸들러).

---

## 7. 로그인 및 매칭 이력 독립 파기 배치

- `MemberLoginHistoryPurgeScheduler`는 통신비밀보호법에 따라 탈퇴 여부와 무관하게 전 회원 대상으로, 로그인 이력을 3개월 후 자동 파기합니다.
- `MatchingHistoryPurgeScheduler`는 탈퇴 후 1년이 지난 회원의 매칭 지원 및 제안 이력을 파기합니다. `matching.member`(지원 및 제안한 개발자) 쪽만 대상이며, 프로젝트 소유자 및 PM 쪽은 건드리지 않습니다.

두 배치 모두 하드삭제 배치와는 독립적으로 동작하며, `member.hard-delete.enabled` 설정과 무관하게 항상 켜져 있습니다.

---

## 미구현 영역

| 항목 | 상태 |
|---|---|
| 탈퇴 시 소유 프로젝트 처리 | 없음. 프로젝트 노출/비노출 기능 병합 후 설계 필요 |
| 환불 신청과 실제 결제(`Payment`, `PaymentRefund`) 연동 | 없음. 잔여 생성권이 어느 결제 건에서 나왔는지 추적하지 않는 구조라 자동 연동이 불가능합니다. 관리자가 수동으로 결제 내역을 대조해 판단합니다 |
| `EXPIRED` 처리된 환불 신청에 대한 회원 통지 | 없음. 하드삭제 시점엔 이미 회원에게 연락할 email이나 PII가 남아있지 않습니다 |
