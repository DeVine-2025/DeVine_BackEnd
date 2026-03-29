# PortOne 결제 시스템 가이드

## 아키텍처 개요

```
Client (프론트엔드)              devine-api                          PortOne
┌──────────────────┐         ┌──────────────────────┐         ┌──────────────┐
│ PortOne SDK      │         │ PaymentController    │         │ V2 API       │
│ → 결제 UI 호출    │────────>│ → 결제 완료 검증      │────────>│ → 결제 조회   │
│                  │         │ → 금액 위변조 확인     │<────────│              │
│                  │         │ → 티켓 지급           │         │              │
└──────────────────┘         └──────────────────────┘         └──────┬───────┘
                                      ▲                              │
                                      │ Webhook (POST)               │
                             ┌────────┴─────────────┐                │
                             │ WebhookController    │<───────────────┘
                             │ → 서명 검증 (HMAC)    │  Transaction.Paid
                             │ → 결제 상태 동기화     │
                             └──────────────────────┘
```

## API 엔드포인트

### 결제 API (`/api/v1/payments`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| POST | `/complete` | 결제 완료 처리 및 티켓 지급 | O |
| GET | `/my` | 내 결제 내역 조회 | O |
| GET | `/channel-key?pg={PgProvider}` | PG사별 채널키 조회 | O |
| POST | `/webhook` | PortOne 웹훅 수신 | X |

### 티켓 API (`/api/v1/tickets`)

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| GET | `/products` | 판매중인 티켓 상품 조회 | X |
| GET | `/my-credits` | 내 잔여 생성권 조회 | O |

## 결제 완료 플로우

### 1. 클라이언트 결제 → completePayment

```
1. 멱등성 검사: portonePaymentId UNIQUE 제약으로 중복 방지
2. 상품 유효성 검증: 티켓 상품 존재 여부, 활성 상태, 중복 상품 확인
3. 서버 금액 계산: Σ(상품가격 × 수량) == 요청 금액 일치 여부
4. PortOne API 조회: 실제 결제 정보 확인
5. 결제 상태 확인: status == "PAID"
6. 소유자 검증: customData.memberId == 현재 사용자
7. 트랜잭션 내 저장: Payment, Transaction, PaymentTicket 생성 + 크레딧 지급
```

### 2. 웹훅 결제 → handleWebhookPayment

```
1. 서명 검증: HMAC-SHA256 (Webhook-Id + Timestamp + Body)
2. 타임스탬프 검증: 5분 이내 요청만 허용 (리플레이 방지)
3. 이벤트 필터: "Transaction.Paid" 이벤트만 처리
4. 멱등성: 이미 처리된 결제는 조기 리턴
5. PortOne API로 결제 정보 재확인
6. customData에서 memberId, items 파싱
7. 금액 검증 + 크레딧 지급
```

> 비즈니스 에러(PaymentException)는 HTTP 200 반환 → PortOne 재시도 방지
> 일시적 에러는 예외 전파 → PortOne 자동 재시도

## 엔티티 구조

### Payment (결제)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| portonePaymentId | String (UNIQUE) | PortOne 결제 ID |
| member | Member (FK) | 결제 회원 |
| orderName | String | 주문명 |
| amount | Long | 결제 금액 (KRW) |
| currency | String | 통화 (기본: KRW) |
| transactions | List\<Transaction> | 거래 내역 |
| paymentTickets | List\<PaymentTicket> | 구매한 티켓 |

### Transaction (거래)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| portoneTransactionId | String (UNIQUE) | PortOne 거래 ID |
| type | `PAYMENT`, `REFUND` | 거래 유형 |
| status | `PAID`, `FAILED`, `REFUNDED`, `CANCELLED` | 거래 상태 |
| method | `CARD`, `EASY_PAY`, `TRANSFER`, `VIRTUAL_ACCOUNT` | 결제 수단 |
| pgProvider | String | PG사 (e.g., KCP_V2) |
| amount | Long | 거래 금액 |
| cardDetail | CardDetail | 카드 상세 (nullable) |
| easyPayDetail | EasyPayDetail | 간편결제 상세 (nullable) |

### 티켓 도메인

```
TicketProduct (상품)          PaymentTicket (구매 내역)       MemberReportCredit (잔여 크레딧)
├─ name: "생성권 1개"         ├─ payment (FK)               ├─ member (FK, UNIQUE)
├─ price: 4900              ├─ ticketProduct (FK)          └─ remainingCount: Integer
├─ creditAmount: 1          ├─ quantity: Integer
└─ active: Boolean          ├─ unitPrice: Long
                            └─ unitCreditAmount: Integer
```

**기본 상품 데이터:**

| 상품명 | 가격 | 크레딧 |
|--------|------|--------|
| 리포트 생성권 1개 | 4,900원 | 1 |
| 리포트 생성권 3개 | 9,900원 | 3 |

## 요청/응답 DTO

### 결제 완료 요청

```json
{
  "paymentId": "payment_1234567890",
  "orderName": "리포트 생성권 1개 x1",
  "amount": 4900,
  "items": [
    { "ticketProductId": 1, "quantity": 1 }
  ]
}
```

### 결제 응답

```json
{
  "paymentId": "payment_1234567890",
  "orderName": "리포트 생성권 1개 x1",
  "amount": 4900,
  "currency": "KRW",
  "createdAt": "2026-03-29T12:00:00",
  "transactions": [
    {
      "transactionId": "txn_abc123",
      "type": "PAYMENT",
      "amount": 4900,
      "status": "PAID",
      "method": "CARD",
      "pgProvider": "KCP_V2",
      "paidAt": "2026-03-29T12:00:00",
      "cardDetail": {
        "cardName": "신한카드",
        "cardNumber": "1234-****-****-5678",
        "cardBrand": "VISA",
        "approvalNumber": "12345678",
        "installmentMonth": 0
      }
    }
  ]
}
```

### 채널키 응답

```json
{
  "channelKey": "channel-key-xxxx-xxxx",
  "pgProvider": "NHN_KCP"
}
```

## 에러 코드

| 코드 | HTTP | 메시지 |
|------|------|--------|
| PAYMENT400_1 | 400 | 이미 처리된 결제입니다 |
| PAYMENT400_2 | 400 | 결제가 완료되지 않았습니다 |
| PAYMENT400_3 | 400 | 결제 금액이 일치하지 않습니다 |
| PAYMENT400_4 | 400 | 지원하지 않는 결제 수단입니다 |
| PAYMENT400_5 | 400 | 웹훅 요청 본문이 올바르지 않습니다 |
| PAYMENT400_6 | 400 | 동일한 상품이 중복으로 포함되어 있습니다 |
| PAYMENT401_1 | 401 | 웹훅 서명이 유효하지 않습니다 |
| PAYMENT403_1 | 403 | 결제 소유자가 일치하지 않습니다 |
| PAYMENT404_1 | 404 | 결제 정보를 찾을 수 없습니다 |
| PAYMENT500_1 | 500 | 크레딧 지급에 실패했습니다 |
| PAYMENT502_1 | 502 | 결제 정보를 조회할 수 없습니다 |

## 환경변수

```yaml
portone:
  store-id: ${PORTONE_STORE_ID}
  api-secret: ${PORTONE_API_SECRET}
  base-url: https://api.portone.io
  webhook-secret: ${PORTONE_WEBHOOK_SECRET}    # whsec_ 접두사 포함
  channel-key:
    nhn-kcp: ${PORTONE_CHANNEL_KEY_NHN_KCP}
    kg-inicis: ${PORTONE_CHANNEL_KEY_KG_INICIS}
    kakaopay: ${PORTONE_CHANNEL_KEY_KAKAOPAY}
```

## 보안 설계

| 항목 | 구현 방식 |
|------|----------|
| 금액 위변조 방지 | 서버 측 상품 금액 재계산 + PortOne 실결제 금액 대조 |
| 웹훅 인증 | HMAC-SHA256 서명 검증 (PortOneWebhookVerifier) |
| 리플레이 방지 | 웹훅 타임스탬프 5분 허용 범위 검증 |
| 결제 소유자 검증 | PortOne customData의 memberId와 현재 사용자 대조 |
| 멱등성 | portonePaymentId UNIQUE 제약 + DataIntegrityViolationException 처리 |
| 시큐리티 | 웹훅 엔드포인트 인증 제외 (SecurityConfig permitAll) |

## DB 마이그레이션

| 파일                                                              | 내용 |
|-----------------------------------------------------------------|------|
| `V20260323000000__253_add_payment_tables.sql`                    | payment, transaction, card_detail, easy_pay_detail 테이블 |
| `V20260329000000__253_add_ticket_tables.sql`                    | ticket_product, payment_ticket, member_report_credit 테이블 + 기본 상품 |
| `V20260329100000__253_extend_transaction_check_constraints.sql` | transaction type/status CHECK 제약 확장 (REFUND, CANCELLED 추가) |
