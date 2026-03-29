# 1:1 채팅 구현 가이드

DeVine 플랫폼의 1:1 채팅 기능 구현 가이드입니다. WebSocket(STOMP) 기반 실시간 메시지 송수신, SSE 채팅 뱃지, REST API, Redis Pub/Sub 기반 멀티 인스턴스 메시지 전달, Redis Sorted Set 기반 Presence 관리를 포함합니다.

---

## 목차

1. [전체 아키텍처](#1-전체-아키텍처)
2. [모듈 및 패키지 구조](#2-모듈-및-패키지-구조)
3. [데이터베이스 스키마](#3-데이터베이스-스키마)
4. [Core Domain (devine-core)](#4-core-domain-devine-core)
5. [REST API (devine-realtime)](#5-rest-api-devine-realtime)
6. [WebSocket 인증](#6-websocket-인증)
7. [STOMP 메시지 핸들러](#7-stomp-메시지-핸들러)
8. [Presence 관리](#8-presence-관리)
9. [Redis Pub/Sub 이벤트 전달](#9-redis-pubsub-이벤트-전달)
10. [SSE 뱃지 연동](#10-sse-뱃지-연동)
11. [예외 처리](#11-예외-처리)
12. [설정 파일 변경 사항](#12-설정-파일-변경-사항)
13. [전체 흐름도](#13-전체-흐름도)
14. [주요 설계 결정 및 주의사항](#14-주요-설계-결정-및-주의사항)
15. [테스트 가이드](#15-테스트-가이드)

---

## 1. 전체 아키텍처

```
[Client A]                              [Client B]
    │                                       │
    │ STOMP over WebSocket                  │ STOMP over WebSocket
    │ /ws/chat                              │ /ws/chat
    ▼                                       ▼
┌─────────────────────────────────────────────────────────┐
│                   devine-realtime (port 8081)            │
│                                                          │
│  [ChatMessageController] ── @MessageMapping             │
│          │                                               │
│  [ChatCommandService]  ── @Transactional                │
│          │                                               │
│  [ChatPresenceManager] ── Redis Sorted Set              │
│          │                                               │
│  AFTER_COMMIT → Redis Pub: chat:user:{receiverId}       │
│          │                                               │
│  [ChatEventSubscriber] ← Redis Sub: chat:user:*         │
│          │                                               │
│  [SimpMessagingTemplate] → /user/queue/chat/{roomId}/messages
│  [SseEmitterManager]    → SSE chat_unread_rooms         │
└─────────────────────────────────────────────────────────┘
            │
            │ REST API
            ▼
┌─────────────────────────────────────────────────────────┐
│                   devine-realtime (port 8081)            │
│  [ChatController]                                        │
│  POST   /api/v1/chat/rooms          채팅방 생성/조회      │
│  GET    /api/v1/chat/rooms          채팅방 목록           │
│  GET    /api/v1/chat/rooms/{id}/messages  메시지 히스토리 │
│  PATCH  /api/v1/chat/rooms/{id}/read      읽음 처리       │
│  DELETE /api/v1/chat/rooms/{id}           방 나가기       │
│  GET    /api/v1/chat/unread-count         전체 미읽음 수  │
└─────────────────────────────────────────────────────────┘
```

**핵심 설계 원칙:**
- **WebSocket(STOMP)**: 실시간 메시지 전달. SockJS 없이 순수 WebSocket만 사용
- **REST API**: 채팅방 생성, 목록 조회, 메시지 히스토리, 읽음 처리는 REST
- **Redis Pub/Sub**: `chat:user:{memberId}` 채널 구독으로 멀티 인스턴스 지원
- **Redis Sorted Set**: Presence 관리 (누가 어느 방에 접속 중인지)
- **AFTER_COMMIT**: DB 저장 후 트랜잭션 커밋 시에만 Redis 발행 (롤백 시 유령 메시지 방지)

---

## 2. 모듈 및 패키지 구조

채팅 관련 코드는 두 모듈에 걸쳐 있습니다.

### devine-core (공유 도메인)

```
devine-core/src/main/java/com/umc/devine/
├── domain/chat/
│   ├── entity/
│   │   ├── ChatRoom.java               # 채팅방 엔티티
│   │   └── ChatMessage.java            # 채팅 메시지 엔티티
│   ├── repository/
│   │   ├── ChatRoomRepository.java     # 채팅방 JPA 레포지토리
│   │   └── ChatMessageRepository.java  # 채팅 메시지 JPA 레포지토리
│   ├── dto/
│   │   ├── ChatReqDTO.java             # 요청 DTO (REST + STOMP 공유)
│   │   ├── ChatResDTO.java             # 응답 DTO
│   │   └── ChatConverter.java          # 엔티티 → DTO 변환
│   └── exception/
│       ├── ChatException.java          # 채팅 도메인 예외
│       └── code/
│           └── ChatErrorReason.java    # 에러 코드 정의
└── infrastructure/redis/
    ├── ChatEventType.java              # 채팅 이벤트 타입 상수
    ├── RedisEventConstants.java        # SSE/채팅 공통 Redis 상수 (수정)
    └── dto/
        └── ChatEventPayload.java       # Redis Pub/Sub 페이로드
```

### devine-realtime (실시간 서버)

```
devine-realtime/src/main/java/com/umc/devine/
├── domain/chat/
│   ├── controller/
│   │   ├── ChatController.java         # REST 컨트롤러
│   │   └── ChatMessageController.java  # STOMP 메시지 핸들러
│   ├── service/
│   │   ├── command/
│   │   │   ├── ChatCommandService.java
│   │   │   └── ChatCommandServiceImpl.java  # 방 생성, 메시지 전송, 읽음, 나가기
│   │   └── query/
│   │       ├── ChatQueryService.java
│   │       └── ChatQueryServiceImpl.java    # 목록, 히스토리, 미읽음 수
│   └── exception/code/
│       └── ChatSuccessCode.java         # 성공 코드 (realtime 모듈 전용)
└── infrastructure/chat/
    ├── auth/
    │   ├── ChatPrincipal.java           # WebSocket 인증 Principal
    │   └── WebSocketAuthInterceptor.java # STOMP CONNECT 인증 인터셉터
    ├── config/
    │   └── WebSocketConfig.java         # WebSocket/STOMP 설정
    ├── listener/
    │   ├── WebSocketEventListener.java  # STOMP 세션 이벤트 처리
    │   └── ChatSseConnectedEventListener.java  # SSE 연결 시 뱃지 초기화
    ├── presence/
    │   ├── ChatPresenceManager.java     # Redis Sorted Set 기반 Presence
    │   ├── ChatPresenceCleanupScheduler.java  # 만료 Presence 정리
    │   └── ChatPresenceRefreshScheduler.java  # 활성 세션 Presence 갱신
    ├── pubsub/
    │   └── ChatEventSubscriber.java     # Redis Pub/Sub 수신 → WS/SSE 전달
    └── registry/
        └── WebSocketSessionRegistry.java # 세션별 방 구독 추적
```

> **패키지 설계 의도**: `domain/chat/`에는 비즈니스 로직(Service, Controller), `infrastructure/chat/`에는 WebSocket 인프라(인증, 설정, Presence, Pub/Sub)를 분리. SSE가 `infrastructure/sse/`에 있는 것과 동일한 패턴.

---

## 3. 데이터베이스 스키마

**파일:** `devine-core/src/main/resources/db/migration/V20260329120000__254_chat.sql`

```sql
-- 채팅방
CREATE TABLE chat_room (
    chat_room_id    BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    member1_id      BIGINT NOT NULL REFERENCES member(member_id),
    member2_id      BIGINT NOT NULL REFERENCES member(member_id),
    member1_left    BOOLEAN NOT NULL DEFAULT FALSE,
    member2_left    BOOLEAN NOT NULL DEFAULT FALSE,
    member1_left_at TIMESTAMP(6),
    member2_left_at TIMESTAMP(6),
    created_at      TIMESTAMP(6) NOT NULL,
    updated_at      TIMESTAMP(6),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT uq_chat_room_members UNIQUE (member1_id, member2_id),
    CONSTRAINT chk_chat_room_member_order CHECK (member1_id < member2_id)
);

CREATE INDEX idx_chat_room_member1 ON chat_room(member1_id);
CREATE INDEX idx_chat_room_member2 ON chat_room(member2_id);

-- 채팅 메시지
CREATE TABLE chat_message (
    chat_message_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    chat_room_id    BIGINT NOT NULL REFERENCES chat_room(chat_room_id),
    sender_id       BIGINT NOT NULL REFERENCES member(member_id),
    content         VARCHAR(1000) NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMP(6),
    created_at      TIMESTAMP(6) NOT NULL,
    updated_at      TIMESTAMP(6),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_chat_message_room_created ON chat_message(chat_room_id, created_at DESC);
CREATE INDEX idx_chat_message_unread ON chat_message(chat_room_id, sender_id, is_read)
    WHERE is_read = FALSE;
```

### 스키마 설계 포인트

| 제약/인덱스 | 목적 |
|------------|------|
| `CHECK (member1_id < member2_id)` | A→B와 B→A 중복 채팅방 생성을 DB 레벨에서 차단. 애플리케이션에서 `min/max`로 정규화 후 저장. |
| `UNIQUE (member1_id, member2_id)` | 동시 요청 시 레이스 컨디션 방어 (DataIntegrityViolationException catch 패턴과 쌍) |
| `member1_left`, `member2_left` | 유저별 독립 나가기. 한 명이 나가도 상대방은 채팅 계속 가능. |
| `member1_left_at`, `member2_left_at` | 재입장 시 나간 시점 이후 메시지만 표시하기 위한 기준점 |
| `is_read` (단일 boolean) | 1:1 채팅에서 수신자는 항상 1명이므로 단일 필드로 충분 |
| `partial index WHERE is_read = FALSE` | 미읽음 메시지 조회 최적화. Hibernate에서 인식하지 못해도 Flyway 관리면 충분. |

---

## 4. Core Domain (devine-core)

### 4-1. Entity

#### ChatRoom

**파일:** `devine-core/.../domain/chat/entity/ChatRoom.java`

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ChatRoom extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member1_id")
    private Member member1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member2_id")
    private Member member2;

    private boolean member1Left;
    private boolean member2Left;
    private LocalDateTime member1LeftAt;
    private LocalDateTime member2LeftAt;
}
```

**주요 메서드:**

```java
// 나가기 (soft delete)
public void leave(Long memberId) {
    if (member1.getId().equals(memberId)) {
        this.member1Left = true;
        this.member1LeftAt = LocalDateTime.now();
    } else {
        this.member2Left = true;
        this.member2LeftAt = LocalDateTime.now();
    }
}

// 재입장 (나간 유저가 상대방 메시지 받으면 자동 rejoin)
public void rejoin(Long memberId) {
    if (member1.getId().equals(memberId) && member1Left) {
        this.member1Left = false;
        this.member1LeftAt = null;
    } else if (member2.getId().equals(memberId) && member2Left) {
        this.member2Left = false;
        this.member2LeftAt = null;
    }
}

// 방 멤버 확인
public boolean isActiveMember(Long memberId) { ... }

// 상대방 Member 반환
public Member getOtherMember(Long memberId) { ... }

// 둘 다 나간 상태 확인
public boolean isBothLeft() { return member1Left && member2Left; }

// 내가 나간 시점 반환 (메시지 필터링용)
public LocalDateTime getLeftAt(Long memberId) {
    if (member1.getId().equals(memberId)) return member1LeftAt;
    return member2LeftAt;
}
```

#### ChatMessage

**파일:** `devine-core/.../domain/chat/entity/ChatMessage.java`

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ChatMessage extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Member sender;

    private String content;
    private boolean isRead;
    private LocalDateTime readAt;

    // 읽음 처리
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
```

> **OSIV 비활성화 대응**: 두 엔티티 모두 연관 관계가 `FetchType.LAZY`. `open-in-view: false` 환경이므로 Service 레이어에서 반드시 fetch join으로 로딩해야 함. 단순 `findById()` 후 연관 엔티티 접근 시 `LazyInitializationException` 발생.

### 4-2. Repository

#### ChatRoomRepository

```java
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 기존 채팅방 조회 (채팅방 생성 시 중복 검사)
    Optional<ChatRoom> findByMember1IdAndMember2Id(Long member1Id, Long member2Id);

    // 내가 나가지 않은 채팅방 목록 (최근 활동 순, native query)
    @Query(value = """
            SELECT cr.chat_room_id, COALESCE(MAX(cm.created_at), cr.created_at) AS last_activity
            FROM chat_room cr
            LEFT JOIN chat_message cm ON cm.chat_room_id = cr.chat_room_id
            WHERE (cr.member1_id = :memberId AND cr.member1_left = false)
               OR (cr.member2_id = :memberId AND cr.member2_left = false)
            GROUP BY cr.chat_room_id
            ORDER BY last_activity DESC
            """, nativeQuery = true)
    List<Object[]> findActiveRoomIdsSortedByActivity(@Param("memberId") Long memberId);

    // 다수 채팅방 + 멤버 일괄 fetch join (N+1 방지)
    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN FETCH cr.member1
            JOIN FETCH cr.member2
            WHERE cr.id IN :roomIds
            """)
    List<ChatRoom> findRoomsWithMembersByIds(@Param("roomIds") List<Long> roomIds);

    // 단일 채팅방 + 멤버 fetch join
    @Query("""
            SELECT cr FROM ChatRoom cr
            JOIN FETCH cr.member1
            JOIN FETCH cr.member2
            WHERE cr.id = :roomId
            """)
    Optional<ChatRoom> findByIdWithMembers(@Param("roomId") Long roomId);
}
```

#### ChatMessageRepository

```java
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 메시지 히스토리 (Slice, 커서 페이징)
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId ORDER BY cm.createdAt DESC")
    Slice<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    // 재입장 후 메시지 히스토리 (나간 시점 이후만)
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.createdAt > :leftAt ORDER BY cm.createdAt DESC")
    Slice<ChatMessage> findByChatRoomIdAndCreatedAtAfterOrderByCreatedAtDesc(
            @Param("chatRoomId") Long chatRoomId, @Param("leftAt") LocalDateTime leftAt, Pageable pageable);

    // 벌크 읽음 처리 (@Modifying)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessage cm SET cm.isRead = true, cm.readAt = :now WHERE cm.chatRoom.id = :chatRoomId AND cm.sender.id = :senderId AND cm.isRead = false")
    int markAllAsRead(@Param("chatRoomId") Long chatRoomId, @Param("senderId") Long senderId, @Param("now") LocalDateTime now);

    // SSE 뱃지용 - 내가 포함된 방 중 미읽음이 있는 방 수
    @Query("""
            SELECT COUNT(DISTINCT cm.chatRoom.id)
            FROM ChatMessage cm
            JOIN cm.chatRoom cr
            WHERE cm.sender.id != :memberId
              AND cm.isRead = false
              AND (
                (cr.member1.id = :memberId AND cr.member1Left = false)
                OR (cr.member2.id = :memberId AND cr.member2Left = false)
              )
            """)
    long countRoomsWithUnreadMessages(@Param("memberId") Long memberId);

    // 채팅방 목록용 - 각 방별 미읽음 수 일괄 조회
    @Query("""
            SELECT cm.chatRoom.id, COUNT(cm)
            FROM ChatMessage cm
            WHERE cm.chatRoom.id IN :roomIds
              AND cm.sender.id != :memberId
              AND cm.isRead = false
            GROUP BY cm.chatRoom.id
            """)
    List<Object[]> countUnreadPerRoom(@Param("roomIds") List<Long> roomIds, @Param("memberId") Long memberId);

    // 각 방의 마지막 메시지 일괄 조회 (N+1 방지)
    @Query("""
            SELECT cm FROM ChatMessage cm
            JOIN FETCH cm.sender
            WHERE cm.id IN (
                SELECT MAX(cm2.id) FROM ChatMessage cm2
                WHERE cm2.chatRoom.id IN :roomIds
                GROUP BY cm2.chatRoom.id
            )
            """)
    List<ChatMessage> findLastMessagesByRoomIds(@Param("roomIds") List<Long> roomIds);
}
```

### 4-3. DTO

#### ChatReqDTO

```java
public class ChatReqDTO {

    public record CreateRoomReq(
            @NotNull(message = "대상 회원 ID는 필수입니다.")
            Long targetMemberId
    ) {}

    public record SendMessageReq(
            @NotNull(message = "메시지 내용은 필수입니다.")
            @Size(min = 1, max = 1000, message = "메시지는 1~1000자여야 합니다.")
            String content
    ) {}
}
```

#### ChatResDTO (주요 레코드)

| 클래스 | 필드 | 사용처 |
|--------|------|--------|
| `ChatRoomInfo` | roomId, otherMember(id, nickname, image, mainType) | 채팅방 생성 응답 |
| `ChatRoomDetail` | roomId, otherMember, lastMessage, lastMessageAt, unreadCount | 채팅방 목록 항목 |
| `ChatRoomList` | `List<ChatRoomDetail> rooms` | 채팅방 목록 응답 |
| `ChatMessageDetail` | messageId, senderId, senderNickname, senderImage, content, isRead, createdAt | 메시지 히스토리 항목 |
| `MessageList` | `List<ChatMessageDetail> messages`, page, hasNext | 메시지 히스토리 응답 |
| `ReadResult` | unreadRoomCount | 읽음 처리 후 현재 미읽음 방 수 |
| `UnreadRoomCount` | unreadRoomCount | 미읽음 방 수 조회 응답 |

### 4-4. Exception

```java
// ChatException.java (devine-core)
public class ChatException extends DomainException {
    public ChatException(ChatErrorReason reason) {
        super(reason);
    }
}

// ChatErrorReason.java (devine-core)
@Getter
@AllArgsConstructor
public enum ChatErrorReason implements DomainErrorReason {
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404_1", "채팅방을 찾을 수 없습니다."),
    NOT_CHAT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "CHAT403_1", "해당 채팅방의 멤버가 아닙니다."),
    CANNOT_CHAT_SELF(HttpStatus.BAD_REQUEST, "CHAT400_1", "자기 자신과 채팅할 수 없습니다."),
    BOTH_LEFT_ROOM(HttpStatus.BAD_REQUEST, "CHAT400_2", "양쪽 모두 나간 채팅방입니다."),
    TARGET_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404_2", "대상 회원을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

### 4-5. Redis 이벤트 구조

#### ChatEventPayload

Redis Pub/Sub으로 전송되는 페이로드:

```java
@Builder
public record ChatEventPayload(
    String eventId,          // 메시지 ID (CHAT_READ는 null)
    String eventType,        // ChatEventType 상수값
    Long receiverId,         // 수신자 memberId (Redis 채널 결정용)
    String receiverClerkId,  // 수신자 clerkId (STOMP 유저 식별용)
    String senderClerkId,    // 발신자 clerkId (읽음 이벤트 역방향 전달용)
    Object data              // 이벤트별 상이한 데이터
) {}
```

#### ChatEventType (상수)

```java
public class ChatEventType {
    public static final String CHAT_MESSAGE = "CHAT_MESSAGE";
    public static final String CHAT_READ = "CHAT_READ";
}
```

**CHAT_MESSAGE data 구조:**
```json
{
  "messageId": 1,
  "roomId": 42,
  "senderId": 3,
  "senderNickname": "홍길동",
  "senderImage": "https://...",
  "content": "안녕하세요",
  "isRead": false,
  "createdAt": "2026-03-29T10:00:00",
  "unreadRoomCount": 2
}
```

**CHAT_READ data 구조:**
```json
{
  "roomId": 42,
  "readerId": 7
}
```

> `unreadRoomCount`는 Redis payload에는 포함되지만 WebSocket 메시지에는 포함되지 않습니다. SSE 전용 필드입니다. `ChatEventSubscriber`에서 WebSocket 전달 시 제거합니다.

---

## 5. REST API (devine-realtime)

### 5-1. 엔드포인트

| Method | Path | 설명 | 인증 |
|--------|------|------|------|
| `POST` | `/api/v1/chat/rooms` | 채팅방 생성 또는 기존 방 반환 | JWT |
| `GET` | `/api/v1/chat/rooms` | 채팅방 목록 (최근 활동 순) | JWT |
| `GET` | `/api/v1/chat/rooms/{roomId}/messages` | 메시지 히스토리 (커서 페이징) | JWT |
| `PATCH` | `/api/v1/chat/rooms/{roomId}/read` | 안 읽은 메시지 전체 읽음 처리 | JWT |
| `DELETE` | `/api/v1/chat/rooms/{roomId}` | 채팅방 나가기 (soft delete) | JWT |
| `GET` | `/api/v1/chat/unread-count` | 미읽음 채팅방 수 조회 | JWT |

### 5-2. ChatCommandService

**채팅방 생성 (`createOrGetRoom`):**

```
1. 자기 자신에게 채팅 시도 → CANNOT_CHAT_SELF
2. sender, target Member 조회
3. min(a, b) → member1Id, max(a, b) → member2Id 정규화
4. findByMember1IdAndMember2Id() → 기존 방이 있으면
   - 내가 나갔던 상태라면 rejoin() 호출
   - 기존 방 반환
5. 기존 방이 없으면 새 ChatRoom 저장
   - DataIntegrityViolationException → 재조회 (동시 요청 레이스 컨디션 처리)
6. toChatRoomInfo() → 응답
```

**메시지 전송 (`sendMessage`):**

```
1. findByIdWithMembers() - LAZY 방지용 fetch join
2. 방 멤버 검증 → NOT_CHAT_ROOM_MEMBER
3. isBothLeft() → BOTH_LEFT_ROOM
4. receiver가 나간 상태면 → rejoin()
5. ChatMessage 빌드
6. ChatPresenceManager.isInRoom(roomId, receiverId) 확인
   - 수신자가 방에 있으면 → message.markAsRead() (즉시 읽음)
7. chatMessageRepository.save(message)
8. unreadRoomCount 계산 (수신자 기준)
   - 수신자가 방에 있으면 → 0 (이미 읽음 처리됨)
   - 없으면 → countRoomsWithUnreadMessages(receiverId)
9. TransactionSynchronizationManager.registerSynchronization()
   → AFTER_COMMIT: publishChatMessage() → Redis publish
```

**읽음 처리 (`markAsRead`):**

```
1. findByIdWithMembers() - fetch join
2. 방 멤버 검증
3. markAllAsRead(roomId, senderId=상대방Id, now) - 벌크 UPDATE
4. updated > 0 이면 → AFTER_COMMIT: publishReadEvent()
   → Redis publish chat:user:{senderId} (상대방에게 읽음 알림)
5. 현재 미읽음 방 수 반환 (ReadResult)
```

**채팅방 나가기 (`leaveRoom`):**

```
1. findByIdWithMembers() - fetch join
2. 방 멤버 검증
3. room.leave(memberId) - memberXLeft=true, memberXLeftAt=now()
```

### 5-3. ChatQueryService

**채팅방 목록 (`getRoomList`) - 2단계 쿼리 패턴:**

N+1 문제와 GROUP BY + fetch join 충돌을 모두 회피하기 위해 쿼리를 분리합니다.

```
Step 1: findActiveRoomIdsSortedByActivity(memberId)
  → native query로 roomId + last_activity만 조회
  → GROUP BY, ORDER BY를 이 쿼리에서 처리
  → List<Object[]> 반환

Step 2: findRoomsWithMembersByIds(roomIds)
  → JPQL fetch join으로 member1, member2 한번에 로딩
  → N+1 방지

Step 3: countUnreadPerRoom(roomIds, memberId)
  → 각 방별 미읽음 수를 한 쿼리로 일괄 조회

Step 4: findLastMessagesByRoomIds(roomIds)
  → 각 방의 MAX(id) 서브쿼리로 마지막 메시지 일괄 조회
  → JOIN FETCH cm.sender로 발신자 정보도 한번에

Step 5: roomIds 순서대로 ChatRoomDetail 조립
```

> GROUP BY와 fetch join을 같은 JPQL에서 쓰면 `HibernateQueryException: query specified join fetching, but the owner of the fetched association was not present in the select list` 에러가 발생합니다. 따라서 Step 1을 native query로 분리했습니다.

**메시지 히스토리 (`getMessages`):**

```
- leftAt(나간 시점)이 null이면 → 전체 메시지 조회
- leftAt이 있으면 → 그 시점 이후 메시지만 조회
- Slice 기반 커서 페이징 (size=50, 최신순)
```

---

## 6. WebSocket 인증

### WebSocketConfig

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns(allowedOrigins);
        // SockJS 없음 - 순수 WebSocket
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}
```

> CORS는 `RealtimeSecurityConfig`와 `WebSocketConfig` 양쪽에서 별도로 설정해야 합니다.

### ChatPrincipal

```java
public class ChatPrincipal implements Principal {
    private final String clerkId;
    private final Long memberId;

    @Override
    public String getName() {
        return clerkId; // STOMP 유저 식별자 = clerkId
    }
}
```

### WebSocketAuthInterceptor

STOMP CONNECT 시 JWT 인증:

```java
@Override
public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = ...;

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        // Bearer 토큰 검증 실패 시 IllegalArgumentException

        String token = authHeader.substring(7);
        Jwt jwt = jwtDecoder.decode(token);
        String clerkId = jwt.getSubject();

        // JdbcTemplate 사용 (OSIV 비활성화 환경 - SseController 패턴 동일)
        Long memberId = jdbcTemplate.query(
                "SELECT member_id FROM member WHERE clerk_id = ? AND used = 'ACTIVE'",
                rs -> rs.next() ? rs.getLong("member_id") : null,
                clerkId
        );

        ChatPrincipal principal = new ChatPrincipal(clerkId, memberId);
        accessor.setUser(principal);  // 이후 모든 메시지에서 principal 사용 가능
    }
    return message;
}
```

> **JdbcTemplate 사용 이유**: `open-in-view: false` 환경에서 JPA EntityManager는 트랜잭션 밖에서 사용 불가. JdbcTemplate은 커넥션을 즉시 반납하므로 인터셉터처럼 트랜잭션 없는 컨텍스트에서도 안전합니다. `SseController.resolveMemberId()`와 동일한 패턴.

---

## 7. STOMP 메시지 핸들러

### ChatMessageController

```java
@Controller
public class ChatMessageController {

    // 메시지 전송
    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Valid ChatReqDTO.SendMessageReq request,
            Principal principal
    ) {
        ChatPrincipal chatPrincipal = (ChatPrincipal) principal;
        chatCommandService.sendMessage(chatPrincipal.getMemberId(), roomId, request.content());
    }

    // 읽음 처리 (방 열었을 때 호출)
    @MessageMapping("/chat/{roomId}/read")
    public void markAsRead(
            @DestinationVariable Long roomId,
            Principal principal
    ) {
        ChatPrincipal chatPrincipal = (ChatPrincipal) principal;
        chatCommandService.markAsRead(chatPrincipal.getMemberId(), roomId);
    }

    // WebSocket 에러 핸들러 - /user/queue/errors 로 전송
    @MessageExceptionHandler
    public void handleException(Exception ex, Principal principal) {
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    Map.of("error", ex.getMessage())
            );
        }
    }
}
```

### STOMP 구독 경로 정리

| 클라이언트 구독 경로 | 수신 이벤트 | 전송자 |
|---------------------|-------------|--------|
| `/user/queue/chat/{roomId}/messages` | 새 메시지 도착 | `ChatEventSubscriber` |
| `/user/queue/chat/read` | 상대방이 내 메시지 읽음 | `ChatEventSubscriber` |
| `/user/queue/errors` | WebSocket 오류 | `ChatMessageController.handleException` |

### STOMP 전송 경로

| 클라이언트 전송 경로 | 처리 핸들러 |
|---------------------|-------------|
| `/app/chat/{roomId}/send` | `ChatMessageController.sendMessage` |
| `/app/chat/{roomId}/read` | `ChatMessageController.markAsRead` |

---

## 8. Presence 관리

### 설계

Redis Sorted Set으로 "누가 어느 방에 접속 중인지" 추적합니다. Sorted Set의 score에 타임스탬프를 저장해 만료 감지에 활용합니다.

```
Key:   chat:presence:{roomId}
Value: memberId (String)
Score: System.currentTimeMillis()

예시:
  chat:presence:42 → { "7": 1743200000000, "3": 1743199990000 }
```

추가로 활성 방 목록을 별도 Set에 유지합니다:
```
Key:   chat:active-rooms
Value: Set<roomId>
```

### ChatPresenceManager

```java
// 방 입장 (SUBSCRIBE 이벤트 시 호출)
public void enterRoom(Long roomId, Long memberId) {
    String key = presenceKeyPrefix + roomId;
    redisTemplate.opsForZSet().add(key, String.valueOf(memberId), System.currentTimeMillis());
    redisTemplate.opsForSet().add(activeRoomsKey, String.valueOf(roomId));
}

// 방 퇴장 (UNSUBSCRIBE 또는 DISCONNECT 이벤트 시 호출)
public void leaveRoom(Long roomId, Long memberId) {
    String key = presenceKeyPrefix + roomId;
    redisTemplate.opsForZSet().remove(key, String.valueOf(memberId));
}

// 접속 여부 확인 (메시지 전송 시 즉시 읽음 여부 결정)
public boolean isInRoom(Long roomId, Long memberId) {
    String key = presenceKeyPrefix + roomId;
    return redisTemplate.opsForZSet().score(key, String.valueOf(memberId)) != null;
}

// Presence 갱신 (주기적 하트비트)
public void refreshPresence(Long roomId, Long memberId) {
    String key = presenceKeyPrefix + roomId;
    redisTemplate.opsForZSet().add(key, String.valueOf(memberId), System.currentTimeMillis());
}
```

### WebSocketSessionRegistry

세션별로 어떤 방을 구독 중인지 추적합니다. DISCONNECT 시 어느 방의 Presence를 정리해야 할지 알기 위해 필요합니다.

```java
// SessionInfo: { memberId, Set<roomId> }
ConcurrentHashMap<String sessionId, SessionInfo>

// CONNECT 시: registerSession(sessionId, memberId)
// SUBSCRIBE 시: addRoom(sessionId, roomId)
// UNSUBSCRIBE 시: removeRoom(sessionId, roomId)
// DISCONNECT 시: removeSession(sessionId) → SessionInfo 반환 (모든 방 Presence 정리)
```

### WebSocketEventListener

STOMP 세션 이벤트 처리:

```java
@EventListener
public void handleSessionConnect(SessionConnectEvent event) {
    // sessionRegistry.registerSession(sessionId, memberId)
}

@EventListener
public void handleSessionSubscribe(SessionSubscribeEvent event) {
    // destination 패턴: /user/queue/chat/{roomId}/messages
    // roomId 추출 → chatPresenceManager.enterRoom(roomId, memberId)
    // sessionRegistry.addRoom(sessionId, roomId)
}

@EventListener
public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
    // chatPresenceManager.leaveRoom(roomId, memberId)
    // sessionRegistry.removeRoom(sessionId, roomId)
}

@EventListener
public void handleSessionDisconnect(SessionDisconnectEvent event) {
    // SessionInfo = sessionRegistry.removeSession(sessionId)
    // SessionInfo.roomIds.forEach → chatPresenceManager.leaveRoom(roomId, memberId)
}
```

**구독 경로에서 roomId 추출:**

```java
private static final Pattern ROOM_ID_PATTERN =
        Pattern.compile("/user/queue/chat/(\\d+)/messages");
```

### 스케줄러

**ChatPresenceCleanupScheduler** (30초마다):
- `chat:active-rooms`에서 방 목록 조회
- 각 방의 Sorted Set에서 `timeout-seconds` 이상 지난 항목 삭제
- Sorted Set이 비면 `chat:active-rooms`에서도 제거

**ChatPresenceRefreshScheduler** (10초마다):
- `WebSocketSessionRegistry`의 활성 세션 전부 순회
- 각 세션의 방에 대해 `refreshPresence()` 호출
- Cleanup 스케줄러(30초)보다 짧은 주기로 갱신해 활성 세션이 만료되지 않도록 보장

---

## 9. Redis Pub/Sub 이벤트 전달

### RealtimeRedisConfig (수정)

```java
// chat:user:* 패턴 구독 등록
container.addMessageListener(chatEventSubscriber,
        new PatternTopic(chatPattern));  // "chat:user:*"
```

### ChatEventSubscriber

모든 realtime 인스턴스에서 `chat:user:*` 패턴을 구독합니다. 이벤트 수신 시 `chatDispatchExecutor`로 비동기 처리합니다.

```java
@Override
public void onMessage(Message message, byte[] pattern) {
    ChatEventPayload payload = objectMapper.readValue(body, ChatEventPayload.class);
    chatDispatchExecutor.execute(() -> dispatch(payload));
}

private void dispatch(ChatEventPayload payload) {
    if (ChatEventType.CHAT_MESSAGE.equals(eventType)) {
        handleChatMessage(payload);
    } else if (ChatEventType.CHAT_READ.equals(eventType)) {
        handleChatRead(payload);
    }
}
```

**CHAT_MESSAGE 처리:**

```java
private void handleChatMessage(ChatEventPayload payload) {
    Map<String, Object> data = (Map<String, Object>) payload.data();

    // WebSocket 전달: unreadRoomCount 제거 후 전송
    Map<String, Object> wsData = new HashMap<>(data);
    wsData.remove("unreadRoomCount");
    messagingTemplate.convertAndSendToUser(
            payload.receiverClerkId(),
            "/queue/chat/" + roomId + "/messages",
            wsData
    );

    // SSE 뱃지 업데이트
    long unreadRoomCount = ...;
    if (unreadRoomCount > 0) {
        sseEmitterManager.sendWithId(
                payload.receiverId(),
                null,
                SseEventType.CHAT_UNREAD_ROOMS.getEventName(),
                Map.of("unreadRoomCount", unreadRoomCount)
        );
    }
}
```

**CHAT_READ 처리:**

```java
private void handleChatRead(ChatEventPayload payload) {
    // 발신자(senderClerkId)에게 읽음 알림 전달
    messagingTemplate.convertAndSendToUser(
            payload.senderClerkId(),
            "/queue/chat/read",
            payload.data()
    );
}
```

> `convertAndSendToUser`의 유저 식별자는 `ChatPrincipal.getName()` = clerkId입니다. STOMP는 Principal 기반으로 유저를 식별하므로 receiverClerkId/senderClerkId를 사용합니다.

---

## 10. SSE 뱃지 연동

채팅 미읽음 뱃지는 SSE를 통해 전달됩니다.

### 이벤트 타입

`SseEventType.CHAT_UNREAD_ROOMS` - 이벤트명: `"chat_unread_rooms"`

### SSE 연결 시 초기 뱃지 전송

**ChatSseConnectedEventListener** (`@EventListener`):

SSE 연결 시 `SseConnectedEvent`를 수신해 현재 미읽음 방 수를 즉시 전송합니다.

```java
@EventListener
public void handleSseConnected(SseConnectedEvent event) {
    long unreadRoomCount = chatMessageRepository.countRoomsWithUnreadMessages(event.getMemberId());
    if (unreadRoomCount > 0) {
        sseEmitterManager.sendWithId(
                event.getMemberId(),
                null,
                SseEventType.CHAT_UNREAD_ROOMS.getEventName(),
                Map.of("unreadRoomCount", unreadRoomCount)
        );
    }
}
```

### 새 메시지 수신 시 뱃지 업데이트

`ChatEventSubscriber.handleChatMessage()`에서 WebSocket 메시지 전달 후 SSE 뱃지도 함께 업데이트합니다 (위 9번 참조).

### 클라이언트 SSE 이벤트 수신

```javascript
eventSource.addEventListener('chat_unread_rooms', (e) => {
    const { unreadRoomCount } = JSON.parse(e.data);
    updateChatBadge(unreadRoomCount);
});
```

---

## 11. 예외 처리

### RealtimeExceptionAdvice (수정)

```java
@Slf4j
@RestControllerAdvice
public class RealtimeExceptionAdvice {

    // DomainException (ChatException, AuthException 등) 처리
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        DomainErrorReason reason = ex.getReason();
        return ResponseEntity.status(reason.getStatus())
                .body(ApiResponse.onFailure(reason, null));
    }

    // @Valid 유효성 검사 실패 (CreateRoomReq.targetMemberId @NotNull 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.onFailure(GeneralErrorReason.VALID_FAIL, errors));
    }

    // 미처리 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.onFailure(GeneralErrorReason.INTERNAL_SERVER_ERROR, ex.getMessage()));
    }
}
```

> `ApiResponse`는 `devine-core`에 위치합니다. `devine-realtime`에서 `devine-core`를 의존하므로 별도 복사 없이 사용 가능합니다.

### WebSocket 에러 처리

`ChatMessageController.@MessageExceptionHandler`가 STOMP 메시지 처리 중 발생한 예외를 클라이언트의 `/user/queue/errors`로 전달합니다.

---

## 12. 설정 파일 변경 사항

### core-defaults.yml (수정)

```yaml
redis:
  channel:
    notification-prefix: "notification:user:"
    notification-pattern: "notification:user:*"
    chat-prefix: "chat:user:"          # 채팅 채널 접두사
    chat-pattern: "chat:user:*"        # 채팅 채널 패턴 (Pub/Sub 구독용)
  presence:
    key-prefix: "chat:presence:"       # Presence Sorted Set 키 접두사
    active-rooms-key: "chat:active-rooms"  # 활성 방 Set 키
    timeout-seconds: 30                # Presence 만료 기준 (초)
```

### application.yml (devine-realtime 수정)

```yaml
executor:
  # 기존 SSE executor들...
  chat-dispatch:
    core-pool-size: ${CHAT_DISPATCH_CORE:10}
    max-pool-size: ${CHAT_DISPATCH_MAX:50}
    queue-capacity: ${CHAT_DISPATCH_QUEUE:200}
```

### RealtimeSecurityConfig (수정)

```java
.requestMatchers("/ws/**").permitAll()  // WebSocket 엔드포인트 인증 허용
```

> WebSocket은 Security 레벨이 아닌 `WebSocketAuthInterceptor`(STOMP 레벨)에서 인증합니다.

### RealtimeRedisConfig (수정)

`ChatEventSubscriber`를 `chat:user:*` 패턴으로 등록합니다.

### RealtimeExecutorConfig (수정)

- `@EnableScheduling` 추가 (Presence 스케줄러 활성화)
- `chatDispatchExecutor` 빈 추가

### build.gradle (devine-realtime 수정)

```gradle
implementation 'org.springframework.boot:spring-boot-starter-websocket'
```

---

## 13. 전체 흐름도

### 메시지 전송 흐름

```
[Client A] — STOMP SEND /app/chat/42/send → {content: "안녕"}
    │
    ▼
[ChatMessageController.sendMessage(roomId=42, memberId=3)]
    │
    ▼
[ChatCommandServiceImpl.sendMessage(memberId=3, roomId=42, content)]
    ├─ findByIdWithMembers(42)             → fetch join으로 member1, member2 로딩
    ├─ 멤버 검증 (3이 member1 or member2인지)
    ├─ isBothLeft() → false (계속 진행)
    ├─ receiver(memberId=7) rejoin() 처리  → receiver가 나갔던 상태면 재입장
    ├─ ChatMessage 빌드
    ├─ ChatPresenceManager.isInRoom(42, 7) → false (7번 유저 방 밖)
    ├─ chatMessageRepository.save(message) → DB 저장
    ├─ countRoomsWithUnreadMessages(7)     → unreadRoomCount = 3
    └─ AFTER_COMMIT → publishChatMessage()
              │
              ▼
       Redis PUBLISH "chat:user:7" → ChatEventPayload {
           eventType: "CHAT_MESSAGE",
           receiverId: 7,
           receiverClerkId: "user_7clerk",
           senderClerkId: "user_3clerk",
           data: { messageId, roomId, ..., unreadRoomCount: 3 }
       }
              │
              ▼ (모든 realtime 인스턴스에서 수신)
[ChatEventSubscriber.handleChatMessage()]
    ├─ wsData = data - unreadRoomCount
    ├─ messagingTemplate.convertAndSendToUser("user_7clerk", "/queue/chat/42/messages", wsData)
    └─ sseEmitterManager.sendWithId(7, null, "chat_unread_rooms", {unreadRoomCount: 3})
              │                                         │
              ▼                                         ▼
[Client B] ← STOMP MESSAGE               [Client B] ← SSE event: chat_unread_rooms
/user/queue/chat/42/messages              뱃지 업데이트 (3개)
```

### 읽음 확인 흐름

```
[Client B] — 채팅방 열기 → STOMP SEND /app/chat/42/read
    │
    ▼
[ChatCommandService.markAsRead(memberId=7, roomId=42)]
    ├─ markAllAsRead(42, senderId=3, now)  → UPDATE SET is_read=true (벌크)
    └─ AFTER_COMMIT → publishReadEvent()
              │
              ▼
       Redis PUBLISH "chat:user:3" → ChatEventPayload {
           eventType: "CHAT_READ",
           receiverId: 3,
           senderClerkId: "user_7clerk",   ← 읽은 사람
           receiverClerkId: "user_3clerk", ← 원래 발신자
           data: { roomId: 42, readerId: 7 }
       }
              │
              ▼
[ChatEventSubscriber.handleChatRead()]
    └─ messagingTemplate.convertAndSendToUser("user_3clerk", "/queue/chat/read", data)
              │
              ▼
[Client A] ← STOMP MESSAGE /user/queue/chat/read
UI에서 메시지 옆 "1" 제거
```

### 즉시 읽음 처리 흐름 (수신자가 방에 있을 때)

```
[Client A] — 메시지 전송
    │
[ChatCommandService.sendMessage()]
    ├─ ChatPresenceManager.isInRoom(42, receiverId=7) → true (7번이 방 구독 중)
    ├─ message.markAsRead()  ← DB 저장 전에 isRead=true 설정
    ├─ chatMessageRepository.save(message)
    ├─ unreadRoomCount = 0 (이미 읽음 처리됨)
    └─ AFTER_COMMIT → publishChatMessage({ isRead: true, unreadRoomCount: 0 })
              │
              ▼
[Client B] ← STOMP MESSAGE { isRead: true }  → "1" 표시 없이 메시지 표시
[Client B] ← SSE 업데이트 없음 (unreadRoomCount=0이므로 전송 생략)
```

### 채팅방 나가기 및 재입장 흐름

```
1. [Client A] DELETE /api/v1/chat/rooms/42
   → ChatRoom.member1Left=true, member1LeftAt=now()
   → Client A 채팅 목록에서 방 42 사라짐

2. [Client B] 이후 메시지 전송
   → receiver.rejoin(member1Id) → member1Left=false, member1LeftAt=null
   → Client A 채팅 목록에 방 42 다시 표시 (나간 시점 이후 메시지부터)

3. 만약 [Client A]도 나가고 [Client B]도 나간 상태에서 전송 시도
   → isBothLeft() = true → BOTH_LEFT_ROOM(400) 에러
```

---

## 14. 주요 설계 결정 및 주의사항

### OSIV 비활성화 대응

`open-in-view: false` 설정으로 모든 LAZY 연관관계는 트랜잭션 내에서만 로딩 가능합니다.

**필수 패턴:**
```java
// ❌ 잘못된 사용 - LAZY 연관관계 접근 시 LazyInitializationException
ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(...);
room.getMember1().getNickname();  // 예외 발생!

// ✅ 올바른 사용 - fetch join으로 미리 로딩
ChatRoom room = chatRoomRepository.findByIdWithMembers(roomId).orElseThrow(...);
room.getMember1().getNickname();  // 안전
```

### AFTER_COMMIT 패턴

트랜잭션 롤백 시 Redis 발행이 일어나지 않도록 합니다.

```java
// ❌ 잘못된 사용 - DB 저장 실패해도 Redis 발행됨
chatMessageRepository.save(message);
redisTemplate.convertAndSend(channel, payload);

// ✅ 올바른 사용 - 커밋 후에만 발행
chatMessageRepository.save(message);
TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.convertAndSend(channel, payload);
            }
        }
);
```

### 동시 채팅방 생성 레이스 컨디션

두 유저가 동시에 서로에게 채팅방 생성 요청 시 UNIQUE 제약 위반이 발생합니다.

```java
try {
    return chatRoomRepository.save(newRoom);
} catch (DataIntegrityViolationException e) {
    // UNIQUE 제약 위반 → 이미 생성된 방 반환
    return chatRoomRepository.findByMember1IdAndMember2Id(m1Id, m2Id)
            .orElseThrow(...);
}
```

### JdbcTemplate 인증 패턴

WebSocket 인증(`WebSocketAuthInterceptor`)과 SSE 인증(`SseController`)은 모두 JdbcTemplate으로 멤버를 조회합니다. JPA를 사용하지 않는 이유는 트랜잭션/EntityManager 컨텍스트 밖에서 실행되기 때문입니다.

```java
// queryForObject() ❌ - 결과 없으면 EmptyResultDataAccessException
jdbcTemplate.queryForObject("SELECT member_id ...", Long.class, clerkId);

// jdbcTemplate.query() + ResultSetExtractor ✅ - null 안전
Long memberId = jdbcTemplate.query(
        "SELECT member_id FROM member WHERE clerk_id = ? AND used = 'ACTIVE'",
        rs -> rs.next() ? rs.getLong("member_id") : null,
        clerkId
);
```

### WebSocket unreadRoomCount 제거

Redis payload에는 SSE 뱃지용 `unreadRoomCount`가 포함되지만, WebSocket 메시지에는 불필요합니다. `ChatEventSubscriber`에서 명시적으로 제거합니다.

```java
Map<String, Object> wsData = new HashMap<>(data);
wsData.remove("unreadRoomCount");  // WebSocket 수신자에게 노출 불필요
messagingTemplate.convertAndSendToUser(receiverClerkId, destination, wsData);
```

### 채팅방 목록 쿼리 분리 이유

```java
// ❌ GROUP BY + fetch join 혼용 - HibernateQueryException
@Query("""
    SELECT cr FROM ChatRoom cr
    JOIN FETCH cr.member1 JOIN FETCH cr.member2
    WHERE ...
    GROUP BY cr  // fetch join + group by 충돌
    ORDER BY MAX(cm.createdAt)
""")

// ✅ 분리 - Step 1: native query로 정렬된 id 조회
//           Step 2: JPQL fetch join으로 엔티티 로딩
```

---

## 15. 테스트 가이드

### 클라이언트 연결 (wscat/Postman)

**WebSocket 연결:**
```bash
wscat -c "ws://localhost:8081/ws/chat" \
      -H "Authorization: Bearer {JWT_TOKEN}"
```

**STOMP CONNECT:**
```
CONNECT
Authorization:Bearer {JWT_TOKEN}
accept-version:1.2
heart-beat:10000,10000

^@
```

**구독:**
```
SUBSCRIBE
id:sub-0
destination:/user/queue/chat/42/messages

^@
```

**메시지 전송:**
```
SEND
destination:/app/chat/42/send
content-type:application/json

{"content":"안녕하세요"}
^@
```

### REST API 테스트 순서

```bash
# 1. 채팅방 생성
POST /api/v1/chat/rooms
{"targetMemberId": 7}
→ {"roomId": 42, "otherMember": {...}}

# 2. 메시지 히스토리
GET /api/v1/chat/rooms/42/messages?page=0&size=50&sort=createdAt,desc

# 3. 읽음 처리
PATCH /api/v1/chat/rooms/42/read
→ {"unreadRoomCount": 0}

# 4. 채팅방 목록
GET /api/v1/chat/rooms
→ {"rooms": [{roomId, otherMember, lastMessage, unreadCount}]}

# 5. 전체 미읽음 수
GET /api/v1/chat/unread-count
→ {"unreadRoomCount": 3}

# 6. 채팅방 나가기
DELETE /api/v1/chat/rooms/42
```

### 시나리오 테스트

| 시나리오 | 검증 항목 |
|----------|-----------|
| 두 클라이언트 실시간 메시지 | A 전송 → B WebSocket 수신 확인 |
| 읽음 확인("1" 제거) | B가 읽음 처리 → A `/user/queue/chat/read` 수신 |
| 수신자 방 접속 중 즉시 읽음 | 메시지 `isRead=true`로 전달되는지 확인 |
| SSE 뱃지 업데이트 | 새 메시지 → SSE `chat_unread_rooms` 이벤트 수신 |
| 동시 채팅방 생성 | A→B, B→A 동시 요청 시 방이 1개만 생성되는지 |
| 나가기 + 재입장 | A 나가기 → B 메시지 전송 → A에 방 재표시 |
| 둘 다 나간 후 전송 | `BOTH_LEFT_ROOM(400)` 에러 수신 확인 |
| WebSocket 인증 실패 | 토큰 없이 CONNECT → 연결 거부 확인 |
| 방 멤버 아닌 유저 전송 | `/user/queue/errors`로 에러 수신 확인 |
| SSE 연결 시 초기 뱃지 | SSE 연결 즉시 `chat_unread_rooms` 이벤트 확인 |
