## 연관된 이슈

- issue #328

## 작업 내용

관리자 신고/유저관리 TODO 2건을 해결하고, 회원 자진 탈퇴 기능을 신규로 추가합니다. 커밋은 아래 순서로 나눠져 있습니다.

### 1. 선행 작업

| 커밋 | 내용 |
|---|---|
| `[Refactor/core]` 회원 상태변경 이력을 admin.member에서 domain.member로 이동 | `MemberStatusHistory`, `MemberStatusAction`, `MemberStatusHistoryRepository`를 `domain.member` 패키지로 이동했습니다. 테이블/컬럼 변경은 없습니다. domain 계층이 admin 계층을 의존하는 구조를 피하기 위함입니다. |

### 2. 관리자 TODO 해결

| 커밋 | 내용 |
|---|---|
| `[Feat/api]` 신고 처리 SUSPEND 액션에 계정 정지 기능 연동 | `ComplaintCommandServiceImpl`에서 세부 액션이 SUSPEND일 때 `AdminMemberCommandService.changeStatus`를 호출하도록 연동했습니다. 피신고자가 이미 정지/탈퇴 등 최종 상태면 조용히 건너뛰는 멱등 처리를 포함합니다(없으면 재신고 처리 시 신고 상태 변경 자체가 롤백됩니다). `AdminMemberCommandService`에 이미 조회된 `Member`를 그대로 받는 오버로드를 추가해 중복 조회도 없앴습니다. |
| `[Test/api]` 신고 처리 SUSPEND 액션 테스트 추가 | 정상 정지 처리, 멱등 케이스(신고 자체의 action/resolver/ComplaintHistory 기록까지 검증)를 확인합니다. |
| `[Feat/api]` 유저 상세조회에 신고이력 연동 | `ComplaintQueryService`에 `getRespondentHistory` 메서드를 추가하고, `AdminMemberQueryServiceImpl.getMemberDetail`에서 이를 호출하도록 연동했습니다. |
| `[Test/api]` 유저 상세조회 신고이력 연동 테스트 추가 | |

### 3. 회원 자진 탈퇴 기능

| 커밋 | 내용 |
|---|---|
| `[Feat/api]` 회원 자진 탈퇴 API 추가 | `POST /api/v1/members/me/withdrawal`, `GET /api/v1/members/me/withdrawal/preview` 엔드포인트를 추가했습니다. PII 즉시 익명화, GitHub 연동 데이터 즉시 삭제, 리포트 생성권/쿠폰 즉시 소멸, 본인이 신고자인 신고 이력 삭제를 하나의 트랜잭션으로 처리합니다. 동시 요청이 경쟁하지 않도록 행 잠금(PESSIMISTIC_WRITE)을 적용했습니다. |
| `[Test/api]` 회원 자진 탈퇴 기능 테스트 추가 | |
| `[Feat/api]` 환불신청 관리자 API 추가 | `admin.ticket` 패키지를 신설해 `GET/PATCH /admin/v1/ticket/refunds`를 추가했습니다. `CreditRefundRequest`가 실제로 devine-core의 `domain.ticket` 패키지에 속해 있어 관리자 API도 그 도메인 이름을 그대로 따랐습니다. |
| `[Test/api]` 환불신청 관리자 API 테스트 추가 | |
| `[Feat/core]` 강제탈퇴자 재가입 제한 이메일 해시 블랙리스트 추가 | 이용약관 제5조 3항에 따라, 강제탈퇴 확정 시 이메일 해시를 1년간 보관하고 회원가입 시 대조해서 재가입을 막습니다. HMAC-SHA256(pepper)으로 계산해 DB 유출 시 원본 이메일 역산을 어렵게 했습니다. |
| `[Test/core]` 이메일 해시 블랙리스트 테스트 추가 | |
| `[Feat/api]` 회원 Hard Delete 배치 추가 | 탈퇴 유예기간이 지난 계정의 개인정보 관련 데이터를 완전 삭제합니다. `MemberHardDeleteHandler` 전략 패턴으로 설계해, 도메인별 정리 작업을 Order가 매겨진 개별 컴포넌트로 분리했습니다. `credit_refund_request`는 금전 청구 기록이라 무조건 삭제하지 않고, 미처리 건은 소멸 상태로 전이한 뒤 회원 참조만 끊습니다. |
| `[Test/api]` 회원 Hard Delete 배치 테스트 추가 | |

### 4. 함께 발견하고 고친 결함

| 커밋 | 내용 |
|---|---|
| `[Fix/core]` 강제탈퇴 확정 시 PII 즉시 익명화 및 회원별 트랜잭션 격리 | 강제탈퇴 확정 시 계정 상태만 바뀌고 PII는 그대로 노출되던 문제를 자진탈퇴와 동일하게 즉시 익명화하도록 수정했습니다. 확정 스케줄러가 전체 배치를 하나의 트랜잭션으로 처리해 한 회원의 실패가 다른 회원까지 롤백시키던 문제도 회원별 독립 트랜잭션(REQUIRES_NEW)으로 격리했습니다. |
| `[Test/api]` 강제탈퇴 확정 스케줄러 테스트 보강 | |
| `[Feat/api]` 로그인 이력, 매칭 이력 독립 파기 배치 추가 | 로그인 이력(3개월, 통신비밀보호법)과 매칭 지원/제안 이력(탈퇴 후 1년)에 대한 독립 파기 배치가 없던 문제를 해결했습니다. |
| `[Test/api]` 로그인 이력, 매칭 이력 파기 배치 테스트 추가 | |
| `[Fix/api]` 문구/주석에서 가운뎃점, em dash 제거 | 리포지토리 작성 컨벤션에 맞춰 정리했습니다. |

### 5. 문서

| 커밋 | 내용 |
|---|---|
| `[Docs]` 회원 자진 탈퇴 관련 문서 추가 | `docs/member-withdrawal-api.md`(API/배치 전체 스펙), `docs/followup-316-project-visibility-and-withdrawal.md`(`#316` 병합 후 후속 작업) |

## 테스트 결과

`./gradlew :devine-core:test :devine-api:test` 전체 통과했습니다 (179개 테스트 파일, 실패 0건).

## 변경 사항 체크리스트

- [x] 코드에 영향이 있는 모든 부분에 대한 테스트를 작성하고 실행했나요?
- [x] 문서를 작성하거나 수정했나요? (`docs/member-withdrawal-api.md`)
- [x] 코드 컨벤션에 따라 코드를 작성했나요?
- [x] 본 PR에서 발생할 수 있는 모든 의존성 문제가 해결되었나요?

## 스크린샷 (선택)

## 리뷰 요구사항 (선택)

**`#302`(관리자 결제 전액환불) 병합분과의 패키지명 충돌 처리**

이 브랜치를 파기 직전 `origin/dev`에 `#302`(관리자가 완료된 결제 건을 PortOne 취소 연동으로 전액 환불하는 기능)가 먼저 병합됐습니다. `#302`가 `com.umc.devine.admin.payment` 패키지에 동일한 클래스명(`AdminPaymentController` 등)을 추가해, 이 PR의 환불 신청 기능도 처음엔 같은 패키지를 쓰다가 충돌이 발견되어 `com.umc.devine.admin.ticket`으로 이름을 바꿨습니다. 두 기능은 완전히 다릅니다. `#302`는 완료된 결제 건의 PG 취소이고, 이 PR은 탈퇴 시 미사용 생성권 환불 신청 접수/처리입니다.

**`feat/api/#316-project-visibility` 병합 시 충돌 예상 안내**

이 브랜치는 아직 dev에 병합되지 않았습니다. 병합 시 수동 병합이 필요한 파일과 체크리스트는 `docs/followup-316-project-visibility-and-withdrawal.md`에 정리해뒀습니다. 요약하면:

- `ComplaintCommandServiceImpl.java`: 두 브랜치가 같은 SUSPEND 처리 블록과 `updateStatus` 파라미터 타입을 각자 건드려서 수동 병합이 필요합니다.
- `ComplaintCommandServiceTest.java`: 두 브랜치가 추가한 테스트가 같은 파일에서 겹칩니다.
- `ApiSecurityConfig.java`: 이 PR과 무관하게, `#316`이 오래된 브랜치라 이미 dev와 어긋나 있어 발생하는 충돌입니다.

**정책 결정 사항 (확인 완료)**

- 본인확인은 비밀번호 재입력 대신 닉네임 재입력으로 처리합니다. Clerk 기반 인증이라 비밀번호 필드 자체가 없습니다.
- 탈퇴 시 채팅 메시지는 별도 삭제 없이, 발신자(Member) 익명화로 상대방에게는 발신자만 익명 처리됩니다.
- 신고 이력은 본인이 신고자였던 건만 삭제하고, 본인이 피신고자였던 건은 보존합니다(다른 신고자의 기록 보호).
- 환불 신청 기록은 미사용 생성권이 어느 결제 건에서 나왔는지 추적하지 않는 구조라, 실제 결제 환불(`#302`)과 자동으로 연동하지 않습니다. 관리자가 필요 시 수동으로 결제 내역을 대조합니다.

## 📎 참고 자료 (선택)

- `docs/member-withdrawal-api.md`: 이 PR이 구현한 API/배치 전체 스펙
- `docs/followup-316-project-visibility-and-withdrawal.md`: `#316` 병합 후 후속 작업
- 선행 PR
  - 관리자 로그인 인증/인가 골격
  - 관리자 유저 관리 기능 추가
  - 관리자 쿠폰/신고 인증 적용 (동일 파일 `ComplaintCommandServiceImpl`을 이 PR도 수정합니다)
