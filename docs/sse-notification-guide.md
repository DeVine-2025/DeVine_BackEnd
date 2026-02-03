# SSE 실시간 알림 시스템 가이드

## 개요

Server-Sent Events(SSE)와 Redis Pub/Sub을 활용한 실시간 알림 시스템입니다.

### 관련 커밋
- `10157fa` - 알림 도메인 기본 기능 구현
- `83bc4c7` - SSE 알림 시스템 인프라 구현
- `5c2be0c` - SSE 및 비동기 설정 추가
- `8ed133b` - 환경별 yml 설정 정리
- `e3bd3be` - SSE 알림 서비스 연동

---

## 아키텍처

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Client    │◄────│   Server    │◄────│    Redis    │
│  (Browser)  │ SSE │  (Spring)   │ Sub │  Pub/Sub    │
└─────────────┘     └─────────────┘     └─────────────┘
                           │                   ▲
                           │ Pub               │
                           └───────────────────┘
```

### 이벤트 흐름

```
[알림 생성 흐름]
MatchingCommandService          NotificationCommandService         EventListener              SseEventPublisher
       │                                │                               │                           │
       │ create(MATCHING_APPLIED, ...)  │                               │                           │
       │───────────────────────────────>│                               │                           │
       │                                │ save(notification)            │                           │
       │                                │ publishEvent(Created)         │                           │
       │                                │──────────────────────────────>│ @TransactionalEventListener
       │                                │                               │ (AFTER_COMMIT, @Async)    │
       │                                │                               │ publishNotification()     │
       │                                │                               │──────────────────────────>│
       │                                │                               │                           │ Redis Pub
                                                                                                    ▼
[SSE 전송 흐름]                                                                            ┌─────────────┐
SseEventSubscriber         SseEmitterManager        Client                                │    Redis    │
       │                          │                    │                                   │  Pub/Sub    │
       │ onMessage()              │                    │     ◄─────────────────────────────└─────────────┘
       │ (Redis Sub)              │                    │
       │ sendWithId()             │                    │
       │─────────────────────────>│                    │
       │                          │ SSE event          │
       │                          │───────────────────>│
```

### 주요 컴포넌트

| 컴포넌트 | 패키지 | 역할 |
|----------|--------|------|
| `SseEmitterManager` | infrastructure.sse | SSE 연결 생성/관리, 클라이언트에게 이벤트 전송 |
| `SseEventPublisher` | infrastructure.sse | Redis Pub/Sub으로 이벤트 발행 |
| `SseEventSubscriber` | infrastructure.sse | Redis 메시지 구독 및 SSE 전송 |
| `SseHeartbeatScheduler` | infrastructure.sse | 연결 유지를 위한 주기적 하트비트 |
| `NotificationCreatedEventListener` | domain.notification | 알림 생성 후 SSE 이벤트 발행 (트랜잭션 커밋 후) |
| `SseConnectedEventListener` | domain.notification | SSE 재연결 시 놓친 알림 전송 |

---

## API 명세

### 1. SSE 연결 (Subscribe)

> SSE 연결 API는 인프라 계층으로 분리되어 `/api/v1/sse` 경로에서 제공됩니다.

```http
GET /api/v1/sse/subscribe
Accept: text/event-stream
Last-Event-ID: {마지막_이벤트_ID} (optional)
```

**응답**: SSE 스트림

**이벤트 타입**:
- `connect` - 연결 성공
- `notification` - 새 알림
- `heartbeat` - 연결 유지 (30초 간격)
- `shutdown` - 서버 종료

### 2. 알림 목록 조회

```http
GET /api/v1/notifications?unreadOnly=false&page=0&size=20
```

**Query Parameters**:
- `unreadOnly` (boolean, default: false) - 읽지 않은 알림만 조회
- `page` (int, default: 0) - 페이지 번호
- `size` (int, default: 20) - 페이지 크기

**응답**:
```json
{
  "isSuccess": true,
  "code": "NOTIFICATION200_1",
  "message": "알림 조회에 성공했습니다.",
  "result": {
    "notifications": [
      {
        "id": 1,
        "type": "MATCHING_APPLIED",
        "title": "새로운 지원자가 있습니다",
        "content": "홍길동님이 프로젝트에 지원했습니다.",
        "referenceId": 123,
        "sender": {
          "id": 2,
          "nickname": "홍길동",
          "profileImageUrl": "https://..."
        },
        "isRead": false,
        "createdAt": "2026-02-01T10:30:00"
      }
    ],
    "hasNext": true,
    "currentPage": 0
  }
}
```

### 3. 읽지 않은 알림 개수

```http
GET /api/v1/notifications/unread-count
```

**응답**:
```json
{
  "isSuccess": true,
  "code": "NOTIFICATION200_1",
  "message": "알림 조회에 성공했습니다.",
  "result": {
    "count": 5
  }
}
```

### 4. 단일 알림 읽음 처리

```http
PATCH /api/v1/notifications/{notificationId}/read
```

### 5. 전체 알림 읽음 처리

```http
PATCH /api/v1/notifications/read-all
```

**응답**:
```json
{
  "isSuccess": true,
  "code": "NOTIFICATION200_3",
  "message": "전체 알림 읽음 처리에 성공했습니다.",
  "result": {
    "markedCount": 10
  }
}
```

---

## 알림 타입 (NotificationType)

| 타입 | 설명 | 카테고리 |
|------|------|----------|
| `MATCHING_APPLIED` | 새로운 지원자가 있습니다 | matching |
| `MATCHING_PROPOSED` | 프로젝트 제안이 도착했습니다 | matching |
| `MATCHING_ACCEPTED` | 지원이 수락되었습니다 | matching |
| `MATCHING_REJECTED` | 지원이 거절되었습니다 | matching |
| `PROJECT_STATUS_CHANGED` | 프로젝트 상태가 변경되었습니다 | project |
| `PROJECT_MEMBER_JOINED` | 새 팀원이 합류했습니다 | project |

---

## 설정 (application.yml)

### 기본 설정 (application.yml)
```yaml
# Redis Channel (공통)
redis:
  channel:
    notification-prefix: "notification:user:"
    notification-pattern: "notification:user:*"
```

### 환경별 설정 (application-dev.yml)
```yaml
# SSE Configuration
sse:
  timeout: 3600000        # SSE 연결 타임아웃 (60분)
  heartbeat-rate: 30000   # 하트비트 주기 (30초)
  executor:
    core-pool-size: 5
    max-pool-size: 10
    queue-capacity: 25

# CORS (WebMvcConfig에서 기본값 제공)
cors:
  allowed-origins:
    - http://localhost:3000
    - http://127.0.0.1:3000
```

### 설정 클래스
- `AsyncConfig`: `@EnableAsync` 활성화
- `RedisConfig`: Redis Pub/Sub 리스너 컨테이너 등록
- `WebMvcConfig`: SSE 비동기 타임아웃, ThreadPool, CORS 설정

---

## 클라이언트 연동 예시

### JavaScript (EventSource)

```javascript
// SSE 연결 (인프라 계층 엔드포인트)
const eventSource = new EventSource('/api/v1/sse/subscribe', {
  withCredentials: true
});

// 연결 성공
eventSource.addEventListener('connect', (event) => {
  console.log('SSE 연결됨:', JSON.parse(event.data));
});

// 새 알림 수신
eventSource.addEventListener('notification', (event) => {
  const notification = JSON.parse(event.data);
  console.log('새 알림:', notification);
  // UI 업데이트 로직
});

// 하트비트
eventSource.addEventListener('heartbeat', (event) => {
  console.log('하트비트:', JSON.parse(event.data));
});

// 서버 종료
eventSource.addEventListener('shutdown', (event) => {
  console.log('서버 종료:', event.data);
  eventSource.close();
});

// 에러 처리
eventSource.onerror = (error) => {
  console.error('SSE 에러:', error);
  // 재연결 로직 (브라우저가 자동 재연결)
};

// 연결 종료
// eventSource.close();
```

### 재연결 시 놓친 알림 받기

브라우저의 EventSource는 자동으로 `Last-Event-ID` 헤더를 포함하여 재연결합니다.
서버는 이 ID 이후의 놓친 알림들을 자동으로 전송합니다.

---

## 테스트 API

개발 환경에서 SSE 알림 시스템을 테스트할 수 있는 API입니다.

### 테스트 알림 전송

```http
POST /api/v1/test/notifications/send
Content-Type: application/json

{
  "receiverId": 1,
  "type": "MATCHING_APPLIED",
  "content": "테스트 알림입니다.",
  "referenceId": 123
}
```

### SSE 연결 상태 확인

```http
GET /api/v1/test/notifications/connections
```

**응답**:
```json
{
  "isSuccess": true,
  "result": {
    "totalConnections": 5,
    "isConnected": true
  }
}
```

---

## 패키지 구조

> SSE 인프라와 Notification 도메인이 분리되어 멀티 모듈 전환에 용이합니다.

```
com.umc.devine
├── domain.notification                        # 알림 도메인 (비즈니스 로직)
│   ├── controller
│   │   ├── NotificationController.java        # 알림 CRUD API
│   │   ├── NotificationControllerDocs.java
│   │   ├── NotificationTestController.java    # 테스트 API (dev/test only)
│   │   └── NotificationTestControllerDocs.java
│   ├── converter
│   │   └── NotificationConverter.java
│   ├── dto
│   │   ├── NotificationReqDTO.java
│   │   └── NotificationResDTO.java
│   ├── entity
│   │   └── Notification.java
│   ├── enums
│   │   └── NotificationType.java
│   ├── event
│   │   ├── NotificationCreatedEvent.java      # 알림 생성 이벤트 (ApplicationEvent)
│   │   ├── NotificationCreatedEventListener.java  # 트랜잭션 커밋 후 SSE 발행
│   │   └── SseConnectedEventListener.java     # 재연결 시 놓친 알림 전송
│   ├── exception
│   │   ├── NotificationException.java
│   │   └── code
│   │       ├── NotificationErrorCode.java
│   │       └── NotificationSuccessCode.java
│   ├── repository
│   │   └── NotificationRepository.java
│   └── service
│       ├── command
│       │   ├── NotificationCommandService.java
│       │   └── NotificationCommandServiceImpl.java
│       └── query
│           ├── NotificationQueryService.java
│           └── NotificationQueryServiceImpl.java
├── infrastructure.sse                         # SSE 인프라 (재사용 가능)
│   ├── controller
│   │   ├── SseController.java                 # SSE 연결 API
│   │   └── SseControllerDocs.java
│   ├── dto
│   │   └── SseEventPayload.java
│   └── event
│       ├── SseConnectedEvent.java             # 연결 이벤트 (ApplicationEvent)
│       ├── SseEmitterManager.java
│       ├── SseEventPublisher.java
│       ├── SseEventSubscriber.java
│       ├── SseEventType.java
│       └── SseHeartbeatScheduler.java
└── global.config
    ├── AsyncConfig.java                       # @EnableAsync
    ├── RedisConfig.java                       # Redis Pub/Sub 리스너 등록
    └── WebMvcConfig.java                      # SSE 비동기 및 CORS 설정
```

### 모듈 의존 관계

```
┌─────────────────────────────────────────┐
│  domain.notification                    │  (도메인 모듈)
│  - 알림 비즈니스 로직                   │
│  - CRUD API                             │
│  - 이벤트 리스너 (SSE 연동)             │
└─────────────────┬───────────────────────┘
                  │ depends on
                  ▼
┌─────────────────────────────────────────┐
│  infrastructure.sse                     │  (인프라 모듈)
│  - SSE 연결 관리 (SseEmitterManager)    │
│  - Redis Pub/Sub (Publisher/Subscriber) │
│  - 하트비트 스케줄러                    │
└─────────────────────────────────────────┘
```

---

## 도메인 서비스 연동 예시

### 매칭 서비스에서 알림 생성

```java
@Service
@RequiredArgsConstructor
@Transactional
public class MatchingCommandServiceImpl implements MatchingCommandService {

    private final NotificationCommandService notificationCommandService;

    @Override
    public MatchingResDTO.ProposeResDTO applyToProject(Long memberId, Long projectId) {
        // ... 비즈니스 로직 ...

        Matching savedMatching = matchingRepository.save(matching);

        // PM에게 지원 알림 전송 (트랜잭션 내에서 호출)
        sendApplyNotification(member, project, savedMatching);

        return MatchingConverter.toMatchingResDTO(savedMatching);
    }

    private void sendApplyNotification(Member applicant, Project project, Matching matching) {
        String content = String.format("%s님이 '%s' 프로젝트에 지원했습니다.",
                applicant.getNickname(), project.getName());

        notificationCommandService.create(
                NotificationType.MATCHING_APPLIED,
                project.getMember().getId(),  // receiver: PM
                applicant.getId(),            // sender: 지원자
                content,
                matching.getId()              // referenceId: matchingId
        );
    }
}
```

### 알림 생성 및 SSE 발행 흐름

`NotificationCommandService.create()` 호출 시:
1. `Notification` 엔티티 저장
2. `NotificationCreatedEvent` 발행 (ApplicationEventPublisher)
3. 트랜잭션 커밋 후 `NotificationCreatedEventListener`가 비동기로 처리
4. `SseEventPublisher.publishNotification()`으로 Redis Pub 발행
5. `SseEventSubscriber`가 Redis Sub 수신
6. `SseEmitterManager.sendWithId()`로 클라이언트에 SSE 전송

---

## 트러블슈팅

### 1. SSE 연결이 자주 끊김
- 프록시/로드밸런서의 타임아웃 설정 확인
- `sse.heartbeat-rate` 값을 프록시 타임아웃보다 짧게 설정

### 2. 알림이 실시간으로 오지 않음
- Redis 연결 상태 확인
- `redis.channel.notification-pattern` 설정 확인
- 로그에서 `Redis Pub 발행` / `Redis Sub 수신` 확인

### 3. CORS 에러
- `cors.allowed-origins`에 클라이언트 도메인 추가
- `allowCredentials: true` 설정 시 와일드카드(*) 사용 불가

### 4. 재연결 후 알림 누락
- `Last-Event-ID` 헤더가 제대로 전송되는지 확인
- `findMissedNotifications` 쿼리 동작 확인

---

## 주의사항 및 향후 개선점

### 현재 구현 주의사항

1. **N+1 문제**
   - `findMissedNotifications` 쿼리에서 sender 조회 시 N+1 발생 가능
   - 권장: `JOIN FETCH` 또는 `@EntityGraph` 적용

2. **@Modifying 옵션**
   - `markAllAsRead` bulk update 후 영속성 컨텍스트 동기화
   - `@Modifying(clearAutomatically = true)` 확인 필요

3. **Redis 연결 실패**
   - 현재 로깅만 수행, 알림 누락 가능
   - 재시도 로직 또는 Fallback 전략 고려

### 향후 개선점

- [ ] SecurityContext에서 memberId 추출 (현재 하드코딩)
- [ ] Redis 발행 실패 시 재시도 또는 Dead Letter Queue 적용
- [ ] SSE 연결 메트릭 수집 (Prometheus/Micrometer)
- [ ] 대규모 트래픽 대응을 위한 Redis Cluster 구성
