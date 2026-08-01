# 어드민 결제 API 명세

관리자페이지용 결제 조회·환불 API.

`/admin/**` 경로는 **공개 Swagger 문서에서 제외**(`@Hidden`)되므로 이 문서가 유일한 명세다.
컨트롤러: `devine-api/src/main/java/com/umc/devine/admin/payment/controller/AdminPaymentController.java`

Base URL: `https://api.devine.kr` (운영) / `http://localhost:8080` (로컬)
Base Path: `/admin/v1/payments`

## 공통

### 인증

`Authorization: Bearer {JWT}`

> **주의**: 어드민 인가(role 검증)는 아직 붙지 않았다. 별도 개발 중인 Member role/시큐리티 체계에 위임된 상태이므로,
> 현재는 인증만 통과하면 모든 유저의 결제 내역과 카드 정보에 접근할 수 있다. role 체계 적용 전 운영 배포 금지.

### 응답 봉투

모든 응답은 다음 형태로 감싸진다.

```json
{
  "isSuccess": true,
  "code": "PAYMENT200_1",
  "message": "결제 목록을 성공적으로 조회했습니다.",
  "result": { }
}
```

실패 시 `isSuccess: false`, `code`/`message`는 아래 각 API의 에러 표를 따른다.

### 결제 상태 (`status`)

`payment` 테이블에는 상태 컬럼이 없다. 조회 시점에 PAYMENT 트랜잭션 상태와 **최신** 환불 상태를 조합해 파생한다.
환불 이력이 있으면 결제 트랜잭션 상태와 무관하게 항상 환불 상태가 우선한다.

| 값 | 의미 |
|---|---|
| `PAID` | 환불 이력 없음 + 결제 성공 |
| `FAILED` | 환불 이력 없음 + 결제 미성공 |
| `REFUND_IN_PROGRESS` | 환불 진행 중 (비종료) |
| `REFUNDED` | 환불 완료 |
| `REFUND_FAILED` | PG가 환불 요청을 거절 (종료, 재시도 가능) |
| `REFUND_UNKNOWN` | 환불 결과 불명 (비종료, 대사 대상) |

환불 재시도로 한 결제에 `payment_refund` 로우가 여러 개 생길 수 있으며, `refund_id`가 가장 큰 로우를 기준으로 한다.

---

## 1. 결제 내역 목록 조회

```
GET /admin/v1/payments
```

유저별·상품별 결제 내역을 페이지 단위로 조회한다.

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `memberNickname` | string | N | – | 유저 닉네임. **정확 일치** (부분 검색 아님) |
| `ticketProductId` | number | N | – | 상품 ID. 단건/3개 묶음 등 `ticket_product` 로우 기준 |
| `startDate` | string(`YYYY-MM-DD`) | N | – | 결제일 시작. **해당 일자 포함** |
| `endDate` | string(`YYYY-MM-DD`) | N | – | 결제일 종료. **해당 일자 포함** |
| `page` | number | N | `1` | 1부터 시작 |
| `size` | number | N | `10` | 페이지 크기 |

- 조건은 모두 선택이며, 지정한 것끼리 AND로 결합된다. 전부 생략하면 전체 조회다.
- **기간 필터 기준은 결제 완료 시각(`transaction.paid_at`)** 이지 레코드 생성 시각이 아니다.
  `endDate`는 내부적으로 다음 날 00:00 미만으로 변환되어 종료일 당일이 온전히 포함된다.
- 정렬은 `paidAt` 내림차순(최신순) 고정이다.
- **존재하지 않는 닉네임을 넣어도 404가 아니라 빈 페이지**를 반환한다. 다른 필터와 동작을 일관되게 맞춘 것이다.
- 한 결제에 상품이 여러 개여도 `ticketProductId` 필터로 인해 행이 중복되지 않는다.

### Response `200 OK` — `PAYMENT200_1`

```json
{
  "isSuccess": true,
  "code": "PAYMENT200_1",
  "message": "결제 목록을 성공적으로 조회했습니다.",
  "result": {
    "content": [
      {
        "paymentId": 12,
        "memberId": 7,
        "memberNickname": "홍길동",
        "orderName": "리포트 생성권 3개 묶음",
        "amount": 12000,
        "paidAt": "2026-07-20T14:32:10",
        "status": "PAID"
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

| 필드 | 타입 | 설명 |
|---|---|---|
| `paymentId` | number | 내부 결제 ID. 상세 조회·환불에 사용 |
| `memberId` | number | 유저 ID |
| `memberNickname` | string | 유저 닉네임 |
| `orderName` | string | 주문명 |
| `amount` | number | 결제 금액 (KRW) |
| `paidAt` | string | 결제 완료 일시 |
| `status` | string | 파생 결제 상태 (위 표 참고) |

### 예시

```
# 특정 유저의 7월 결제 내역
GET /admin/v1/payments?memberNickname=홍길동&startDate=2026-07-01&endDate=2026-07-31

# 3개 묶음 상품 결제만 2페이지
GET /admin/v1/payments?ticketProductId=2&page=2&size=20
```

---

## 2. 결제 상세 조회

```
GET /admin/v1/payments/{paymentId}
```

결제 건별 상세(결제수단, 결제일시, 금액)와 해당 유저의 잔여 리포트 생성권 수를 함께 조회한다.

### Path Parameters

| 이름 | 타입 | 설명 |
|---|---|---|
| `paymentId` | number | 내부 결제 ID |

### Response `200 OK` — `PAYMENT200_5`

```json
{
  "isSuccess": true,
  "code": "PAYMENT200_5",
  "message": "결제 상세를 성공적으로 조회했습니다.",
  "result": {
    "paymentId": 12,
    "portonePaymentId": "payment_1753012345",
    "memberId": 7,
    "memberNickname": "홍길동",
    "orderName": "리포트 생성권 3개 묶음",
    "amount": 12000,
    "currency": "KRW",
    "paidAt": "2026-07-20T14:32:10",
    "status": "PAID",
    "method": {
      "method": "CARD",
      "provider": null,
      "cardName": "신한",
      "cardNumber": "123456******3456",
      "cardBrand": "MASTER",
      "approvalNumber": "00012345",
      "installmentMonth": null
    },
    "pgProvider": "TOSSPAYMENTS",
    "tickets": [
      {
        "ticketProductId": 2,
        "productName": "리포트 생성권 3개 묶음",
        "quantity": 1,
        "unitPrice": 12000,
        "unitCreditAmount": 3,
        "totalCredits": 3
      }
    ],
    "remainingReportCredits": 4,
    "refund": null
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `portonePaymentId` | string | PortOne 결제 ID |
| `currency` | string | 통화 |
| `method` | object\|null | 결제수단 상세. PAYMENT 트랜잭션이 없으면 `null` |
| `pgProvider` | string | PG사 |
| `tickets[]` | array | 이 결제로 구매한 상품 목록 |
| `remainingReportCredits` | number | **해당 유저 계정의 현재 잔여 생성권 수** |
| `refund` | object\|null | 환불 이력이 있을 때만 non-null |

#### `method` 객체

`method` 값이 `CARD`면 카드 필드가, `EASY_PAY`면 `provider`(간편결제사) + 카드 필드가 채워진다.

| 필드 | 설명 |
|---|---|
| `method` | `CARD` / `EASY_PAY` / `TRANSFER` / `VIRTUAL_ACCOUNT` |
| `provider` | 간편결제사 (간편결제인 경우에만) |
| `cardName` / `cardNumber` / `cardBrand` | 카드사 / 마스킹된 카드번호 / 브랜드 |
| `approvalNumber` | 승인번호 |
| `installmentMonth` | 할부 개월 (일시불이면 `null`) |

#### `tickets[]` 객체

| 필드 | 설명 |
|---|---|
| `ticketProductId` | 상품 ID (목록 조회 필터에 사용) |
| `productName` | 상품명 |
| `quantity` | 수량 |
| `unitPrice` | 단가 |
| `unitCreditAmount` | 단위당 크레딧 수 |
| `totalCredits` | `quantity × unitCreditAmount` — **이 결제로 지급된** 크레딧 수 |

#### `refund` 객체

| 필드 | 설명 |
|---|---|
| `status` | `IN_PROGRESS` / `COMPLETED` / `FAILED` / `UNKNOWN` |
| `reason` | 환불 사유 |
| `cancellationId` | PortOne 취소 ID (PG 취소가 확인된 경우에만) |
| `failureReason` | 실패/불명 사유 |
| `refundedAt` | 환불 레코드 최종 갱신 일시 |

> **`remainingReportCredits` 해석 주의**
> 이 값은 *이 결제로 지급된 수량*이 아니라 **유저 계정의 현재 잔여량**이다.
> 크레딧은 결제 건별로 쪼개지지 않고 `member_report_credit` 한 로우에 누적되는 구조라 결제별 잔여를 계산할 수 없다.
> 이 결제가 지급한 크레딧을 보려면 `tickets[].totalCredits`를 참고한다.
> 크레딧 로우가 아직 없는 유저면 `0`이다.

### 에러

| HTTP | code | 상황 |
|---|---|---|
| 404 | `PAYMENT404_1` | 해당 `paymentId`의 결제가 없음 |

---

## 3. 결제 환불

```
POST /admin/v1/payments/{paymentId}/refund
```

완료된 결제 건을 **전액** 환불하고, 지급했던 리포트 생성권을 회수한다. 부분 환불은 지원하지 않는다.

### Request Body

```json
{
  "reason": "고객 요청"
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `reason` | string | Y | 공백 불가, 최대 255자 |

### Response `200 OK` — `PAYMENT200_4`

```json
{
  "isSuccess": true,
  "code": "PAYMENT200_4",
  "message": "결제가 성공적으로 환불되었습니다.",
  "result": {
    "cancellationId": "cancellation_1753099999",
    "amount": 12000,
    "cancelledAt": "2026-07-21T09:15:00",
    "revokedCredits": 3
  }
}
```

| 필드 | 설명 |
|---|---|
| `cancellationId` | PortOne 취소 ID |
| `amount` | 환불 금액 (KRW) |
| `cancelledAt` | 취소 완료 시각 |
| `revokedCredits` | 실제로 회수된 크레딧 수 |

> `revokedCredits`는 지급했던 수량과 다를 수 있다. 유저가 이미 크레딧을 써버렸으면 **잔액이 음수가 되지 않도록 남은 만큼만** 회수한다.

### 에러

| HTTP | code | 상황 | 대응 |
|---|---|---|---|
| 404 | `PAYMENT404_1` | 결제 없음 | – |
| 400 | `PAYMENT400_2` | 결제가 완료 상태가 아님 | – |
| 400 | `PAYMENT400_7` | 이미 환불됐거나 환불 처리 중 | 상세 조회로 현재 상태 확인 |
| 502 | `PAYMENT502_2` | PG가 취소를 거절 | **재시도 가능** |
| 504 | `PAYMENT504_1` | 취소 결과 불명 (타임아웃/IO/모호한 PG 응답) | **재시도 금지.** 상태가 `REFUND_UNKNOWN`으로 남으며 대사 후 확정된다 |
| 500 | `PAYMENT500_2` | PG 취소는 됐으나 로컬 반영 실패 | **재시도 금지.** 대사 대상 |

> **재시도 시 주의**
> `PAYMENT504_1`, `PAYMENT500_2`는 PG 쪽에서 실제로 취소가 됐을 수 있는 비종료 상태다. 이중 환불을 막기 위해
> 자동 재시도를 걸지 말고 대사 결과를 기다려야 한다. 확정 실패(`PAYMENT502_2`)만 재시도 대상이다.
> 동시 환불 요청은 `payment_refund`의 활성 상태 부분 유니크 인덱스로 방어되어 `PAYMENT400_7`로 떨어진다.
