# Spring Multi-Module 개발 가이드

## 1. 새로운 기능 추가 시 모듈 배치 기준

### 판단 플로우

```
새 코드를 작성한다
  → API와 Realtime 모두 사용하는가?
      → YES → core에 배치
      → NO  → 해당 모듈에 배치
```

### 계층별 배치 원칙

| 계층 | core | api | realtime |
|------|------|-----|----------|
| Entity | O | X | X |
| Repository | O | X | X |
| Enum / ErrorCode | O | X | X |
| Service | X | O | O |
| Controller | X | O | O |
| DTO (요청/응답) | X | O | O |
| Converter | 양쪽 참조 시 O | 단독 사용 시 O | 단독 사용 시 O |
| Config (Security, Web) | X | O | O |
| Config (Redis, JPA, Async) | O | X | X |
| Scheduler | X | O | O |
| 외부 API 연동 | X | O | X |

---

## 2. Core 비대화 방지

Core가 커지면 빌드 시간 증가, 변경 영향 범위 확대, 관심사 혼재 문제가 발생한다.

### 원칙: Core는 "데이터 계층 + 공유 인프라"만

Core에 넣어야 하는 것:
- 엔티티, 리포지토리, 열거형, 예외
- 두 모듈 이상이 **실제로** 참조하는 공유 코드

Core에 넣으면 안 되는 것:
- **비즈니스 로직** (Service, Helper, Validator)
- **SecurityFilterChain 빈** (모듈마다 엔드포인트가 다름)
- **컨트롤러, DTO** (한쪽 모듈에서만 사용)
- **"나중에 쓸 것 같은" 코드** (YAGNI 원칙)

### 흔한 실수와 대응

| 실수 | 왜 문제인가 | 올바른 방법 |
|------|------------|------------|
| Service를 core에 배치 | core에 비즈니스 로직이 침투 | 각 모듈에 독립적으로 구현 |
| "공유 DTO"를 core에 배치 | API 변경이 core를 거쳐야 함 | DTO는 사용하는 모듈에 배치. 정말 공유가 필요하면 core에 record로 최소화 |
| Converter를 무조건 core에 배치 | core가 DTO에 의존하게 됨 | Converter가 참조하는 DTO가 어디 있는지 확인. DTO가 api에 있으면 converter도 api에 |
| 유틸 클래스를 core에 쌓기 | core가 잡동사니 모듈화 | 한 모듈에서만 쓰는 유틸은 해당 모듈에 배치 |
| Config 클래스를 core에 배치 | 모듈별 설정이 다를 수 있음 | SecurityConfig, WebMvcConfig 등은 각 모듈에. RedisConfig, JpaConfig 등 공유 인프라만 core에 |

### Core에 넣기 전 체크리스트

새 클래스를 core에 추가하려 할 때 아래 질문에 모두 YES여야 한다:

- [ ] API와 Realtime 모두 이 클래스를 **직접** import하는가?
- [ ] 이 클래스가 없으면 두 모듈 중 하나가 **컴파일 실패**하는가?
- [ ] 이 클래스에 비즈니스 로직이 포함되어 있지 않은가?

하나라도 NO면 해당 모듈에 배치한다.

---

## 3. 새로운 도메인 추가 예시

채팅(Chat) 도메인을 추가한다고 가정:

### Core에 추가

```
devine-core/.../domain/chat/
├── entity/
│   ├── ChatRoom.java
│   └── ChatMessage.java
├── enums/
│   └── ChatRoomStatus.java
├── exception/
│   └── code/ChatErrorCode.java
└── repository/
    ├── ChatRoomRepository.java
    └── ChatMessageRepository.java
```

### Realtime에 추가

```
devine-realtime/.../domain/chat/
├── controller/
│   └── ChatController.java
├── dto/
│   ├── ChatReqDTO.java
│   └── ChatResDTO.java
├── converter/
│   └── ChatConverter.java
└── service/
    ├── command/ChatCommandService.java
    └── query/ChatQueryService.java
```

### API에는?

채팅 목록 조회 같은 REST API가 필요하면 api에도 추가:

```
devine-api/.../domain/chat/
├── controller/
│   └── ChatRestController.java
├── dto/
│   └── ChatListResDTO.java
└── service/
    └── query/ChatQueryService.java
```

API와 Realtime의 Service는 **같은 인터페이스를 공유하지 않는다**. 각 모듈의 요구사항이 다르므로 독립적으로 구현한다.

---

## 4. 모듈 간 통신

### 원칙: 모듈 간 직접 의존 금지

API와 Realtime은 서로를 import하지 않는다. 통신이 필요하면 **Redis Pub/Sub**을 사용한다.

### 새로운 이벤트 추가 시

1. **core에 상수 추가** (`RedisEventConstants`):
```java
public static final String CHAT_MESSAGE = "chat_message";
```

2. **core에 Payload DTO 추가** (필요 시):
```java
public record ChatEventPayload(Long roomId, Long senderId, String message) {}
```

3. **발행 모듈**에서 `RedisTemplate.convertAndSend()` 호출

4. **수신 모듈**에서 `MessageListener` 구현

---

## 5. 설정(YAML) 추가 가이드

### 새 환경변수 추가 시

| 사용 범위 | 추가 위치 |
|-----------|-----------|
| 두 모듈 공통 (DB, Redis 등) | `core-defaults.yml` |
| API 전용 (S3, Clerk, FastAPI 등) | `devine-api/application.yml` |
| Realtime 전용 (SSE, WebSocket 등) | `devine-realtime/application.yml` |
| 프로필별 차이 (로깅, 풀 사이즈 등) | `application-dev.yml` / `application-prod.yml` |
| 테스트 전용 | `application-test.yml` |

### .env 주의사항

- `.env`는 프로젝트 루트에 위치 (각 모듈 디렉토리가 아님)
- `bootRun`은 `workingDir = rootProject.projectDir`로 설정되어 있음
- 모듈별로 다른 값이 필요한 환경변수는 변수명을 분리 (예: `SPRING_PORT` vs `REALTIME_PORT`)

---

## 6. 의존성 추가 가이드

### build.gradle에 의존성 추가 시

| 상황 | scope | 위치 |
|------|-------|------|
| 하위 모듈에서도 직접 사용 | `api` | core |
| core 내부에서만 사용 | `implementation` | core |
| 컴파일에만 필요 (Swagger 등) | `compileOnly` | core |
| api 모듈 전용 | `implementation` | api |
| realtime 모듈 전용 | `implementation` | realtime |

**`api` vs `implementation` 판단**: 하위 모듈(api, realtime)에서 해당 라이브러리의 클래스를 직접 import하면 `api`, core 내부에서만 사용하면 `implementation`.

### QueryDSL 관련

Q-클래스는 core에서 생성되고, QueryDSL 구현체도 core에 있다. api/realtime에서 Q-클래스를 직접 참조할 필요가 없도록 설계한다.

---

## 7. 테스트 작성 가이드

### 어느 모듈에 테스트를 작성할까?

| 테스트 대상 | 모듈 | 베이스 클래스 |
|------------|------|--------------|
| Repository | core | `CoreIntegrationTestSupport` |
| Service (api) | api | `IntegrationTestSupport` |
| Controller (api) | api | `ControllerIntegrationTestSupport` |
| Service (realtime) | realtime | `IntegrationTestSupport` |

### 새 테스트 작성 시 주의사항

- **core 테스트**: 반드시 `CoreIntegrationTestSupport`를 상속. `IntegrationTestSupport` 직접 상속 금지 (`@SpringBootConfiguration` 충돌)
- **api 테스트**: `IntegrationTestSupport` 또는 `ControllerIntegrationTestSupport` 상속
- **`@Transactional(propagation = NOT_SUPPORTED)` 테스트**: `@AfterEach`에서 notification 테이블 정리 필수 (비동기 알림 생성으로 인한 FK 제약 위반 방지)

```java
@AfterEach
void cleanup() throws InterruptedException {
    Thread.sleep(500); // 비동기 알림 생성 대기
    jdbcTemplate.execute("DELETE FROM notification");
    // 이후 다른 테이블 정리
}
```

### 테스트 리소스 공유

- `init-pgvector.sql`: core의 testFixtures 리소스에 위치
- `IntegrationTestSupport`: core의 testFixtures에 위치 → api/realtime에서 공유
- `CoreTestApplication`: core의 test에 위치 → api/realtime에 노출되지 않음

---

## 8. Flyway 마이그레이션

- 마이그레이션 파일 위치: `devine-core/src/main/resources/db/migration/`
- **API만 실행**: `devine-api`의 `flyway.enabled=true`
- **Realtime은 실행 안 함**: Flyway 의존성 자체를 exclude
- 새 마이그레이션 파일은 항상 core에 추가

---

## 9. 자주 묻는 질문

### Q: core에 있는 엔티티에 필드를 추가하면?
core만 수정하면 된다. api/realtime은 core JAR을 참조하므로 자동 반영. Flyway 마이그레이션 파일도 core에 추가.

### Q: api에서만 쓰는 엔티티도 core에?
YES. 엔티티는 항상 core에 배치한다. 리포지토리, Flyway 마이그레이션과의 일관성을 위해.

### Q: 공통 예외 처리(GeneralExceptionAdvice)는 어디에?
core에 있다. `@RestControllerAdvice`로 선언되어 있어 각 모듈에서 컴포넌트 스캔 시 자동 적용.

### Q: 새 모듈을 추가하려면?
1. `settings.gradle`에 `include 'devine-{name}'`
2. `devine-{name}/build.gradle` 생성 (api/realtime 참고)
3. `implementation project(':devine-core')` 의존성 추가
4. Application 클래스 생성 (`@SpringBootApplication(scanBasePackages = "com.umc.devine")`)
5. `application.yml`에 `spring.config.import`로 core-defaults.yml 가져오기
