# SSE 실시간 알림 시스템 가이드

## 연결 방식

클라이언트 → SSE 연결 → devine-sse 모듈 (port 8081)
알림 생성 → devine-api 모듈 → Redis Pub/Sub → devine-sse → SSE 전송

```
devine-api                         devine-sse
┌──────────────────┐              ┌──────────────────┐
│ 알림 생성          │  Redis Pub   │ Redis Sub        │
│ → Event 발행      │─────────────>│ → SSE 전송        │──> Client
│                  │              │                  │
└──────────────────┘              └──────────────────┘
                                         ▲
                                         │ SSE 연결
                                       Client
```

### SSE 연결

```http
GET /api/v1/sse/subscribe
Accept: text/event-stream
Last-Event-ID: {마지막_이벤트_ID}  (재연결 시 놓친 알림 자동 수신)
```

**이벤트 타입**:
| 이벤트 | 설명 |
|--------|------|
| `connect` | 연결 성공 |
| `notification` | 새 알림 |
| `heartbeat` | 연결 유지 (30초 간격) |
| `shutdown` | 서버 종료 |

---

## 알림 종류

| 타입 | 설명 | 카테고리 |
|------|------|----------|
| `MATCHING_APPLIED` | 새로운 지원자가 있습니다 | matching |
| `MATCHING_PROPOSED` | 프로젝트 제안이 도착했습니다 | matching |
| `MATCHING_ACCEPTED` | 지원이 수락되었습니다 | matching |
| `MATCHING_REJECTED` | 지원이 거절되었습니다 | matching |
| `PROJECT_STATUS_CHANGED` | 프로젝트 상태가 변경되었습니다 | project |
| `PROJECT_MEMBER_JOINED` | 새 팀원이 합류했습니다 | project |
| `REPORT_COMPLETED` | 리포트 생성이 완료되었습니다 | report |
| `REPORT_FAILED` | 리포트 생성에 실패했습니다 | report |

---

## 알림 생성 타이밍

### Matching

| 타입 | 트리거 시점 | 수신자 |
|------|------------|--------|
| `MATCHING_APPLIED` | 멤버가 프로젝트에 지원 | PM |
| `MATCHING_PROPOSED` | PM이 멤버에게 제안 | 멤버 |
| `MATCHING_ACCEPTED` | 지원/제안 수락 | 상대방 |
| `MATCHING_REJECTED` | 지원/제안 거절 | 상대방 |

### Project

| 타입 | 트리거 시점 | 수신자 |
|------|------------|--------|
| `PROJECT_STATUS_CHANGED` | 프로젝트 상태 변경 | 프로젝트 멤버 |
| `PROJECT_MEMBER_JOINED` | 새 팀원 합류 | PM |

### Report

| 타입 | 트리거 시점 | 수신자 |
|------|------------|--------|
| `REPORT_COMPLETED` | 리포트 생성 완료 (동기/비동기) | 요청자 |
| `REPORT_FAILED` | 리포트 생성 실패 (동기/비동기) | 요청자 |

---

## 알림 생성 흐름

```
NotificationCommandService.create(type, receiverId, senderId, content, referenceId)
  → Notification 엔티티 저장
  → NotificationCreatedEvent 발행 (ApplicationEventPublisher)
  → 트랜잭션 커밋 후 EventListener가 @Async로 Redis Pub 발행
  → devine-sse의 Redis Subscriber가 수신
  → SseEmitterManager로 클라이언트에 SSE 전송
```

---

## 알림 API

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/v1/notifications?unreadOnly=false&page=0&size=20` | 알림 목록 조회 |
| `GET` | `/api/v1/notifications/unread-count` | 읽지 않은 알림 개수 |
| `PATCH` | `/api/v1/notifications/{id}/read` | 단일 읽음 처리 |
| `PATCH` | `/api/v1/notifications/read-all` | 전체 읽음 처리 |
