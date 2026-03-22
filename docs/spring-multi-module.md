# Spring Multi-Module 전환

## 1. 개요

SSE(알림) + 향후 WebSocket(채팅) 기능을 코드 레벨에서 분리하여 관심사를 격리하고, 2 JAR 독립 배포가 가능한 구조로 전환한다.

### 전환 전

```
devine (단일 모듈)
└── src/main/java/com/umc/devine/
    ├── domain/         # 엔티티, 서비스, 컨트롤러 모두 포함
    ├── global/         # 설정, 보안, 유틸
    └── infrastructure/ # 외부 연동 (S3, Clerk, SSE, Redis)
```

### 전환 후

```
devine (루트)
├── devine-core       # 공유 라이브러리 (엔티티, 리포지토리, 공통 설정)
├── devine-api        # REST API 서버 (port 8080)
└── devine-realtime   # SSE 서버 (port 8081)
```

---

## 2. 모듈 구성

### 2-1. devine-core (공유 라이브러리)

**역할**: 엔티티 + 리포지토리 + 공통 설정. 서비스 로직 없음. SecurityFilterChain 빈 없음.

**플러그인**: `java-library`, `java-test-fixtures`
**bootJar**: 비활성 (일반 JAR만 생성)

**포함 패키지**:

| 패키지 | 내용 |
|--------|------|
| `domain/*/entity/` | 모든 도메인 엔티티 (Member, Project, Notification 등) |
| `domain/*/repository/` | JPA 리포지토리 + QueryDSL 구현체 |
| `domain/*/enums/` | 도메인 열거형 |
| `domain/*/exception/` | 도메인 예외 + 에러코드 |
| `domain/notification/converter/`, `dto/` | Realtime에서 참조하므로 core에 배치 |
| `domain/category/converter/` | 다른 도메인 converter에서 참조 |
| `global/apiPayload/` | ApiResponse, ErrorCode, SuccessCode, GeneralException |
| `global/entity/` | BaseEntity (audit 컬럼) |
| `global/security/` | ClerkJwtAuthenticationConverter, CurrentMember, CurrentMemberArgumentResolver |
| `global/config/` | RedisConfig, AsyncConfig, QueryDslConfig, JpaAuditingConfig, CoreExecutorConfig |
| `global/dto/` | PageRequest, PagedResponse |
| `global/validation/` | 커스텀 validation annotation + validator |
| `global/util/` | GitUrlParser |
| `infrastructure/redis/` | RedisEventConstants, SseEventPayload (공유 DTO) |

**주요 의존성** (`api` scope로 하위 모듈에 전이):
- spring-boot-starter-data-jpa, security, oauth2-resource-server, data-redis, validation
- flyway-core, flyway-database-postgresql
- querydsl-jpa 7.0

---

### 2-2. devine-api (REST API 서버)

**역할**: REST 엔드포인트, 비즈니스 로직, 외부 API 연동
**포트**: 8080 (`${API_PORT}`)

**포함 패키지**:

| 패키지 | 내용 |
|--------|------|
| `domain/*/controller/` | REST 컨트롤러 |
| `domain/*/service/` | 비즈니스 서비스 (command/query) |
| `domain/*/converter/` | DTO ↔ 엔티티 변환 |
| `domain/*/dto/` | 요청/응답 DTO |
| `domain/*/event/` | 도메인 이벤트 (ReportCreatedEvent 등) |
| `domain/project/helper/`, `validator/` | 프로젝트 도메인 헬퍼 |
| `global/config/` | ApiSecurityConfig, SwaggerConfig, WebMvcConfig, RestClientConfig |
| `global/filter/` | LoggingFilter |
| `global/scheduler/` | EmbeddingRetryScheduler, WeeklyViewCountResetScheduler, ImageCleanupScheduler |
| `infrastructure/clerk/` | Clerk 인증 연동 |
| `infrastructure/github/` | GitHub API 연동 |
| `infrastructure/fastapi/` | FastAPI 리포트 생성 연동 |
| `infrastructure/s3/` | AWS S3 이미지 업로드 |

**추가 의존성**: spring-boot-starter-web, actuator, springdoc-openapi, spring-cloud-aws-s3

---

### 2-3. devine-realtime (SSE 서버)

**역할**: SSE 연결 관리, Redis Pub/Sub 수신, 실시간 알림 전달
**포트**: 8081 (`${REALTIME_PORT}`)

**포함 패키지**:

| 패키지 | 내용 |
|--------|------|
| `infrastructure/sse/controller/` | SseController (`/sse/v1/subscribe`) |
| `infrastructure/sse/core/` | SseEmitterManager, SseEventType |
| `infrastructure/sse/listener/` | SseConnectedEvent, SseConnectedEventListener |
| `infrastructure/sse/pubsub/` | SseEventSubscriber (Redis MessageListener) |
| `global/config/` | RealtimeSecurityConfig, RealtimeRedisConfig, RealtimeExecutorConfig, RealtimeWebMvcConfig |
| `global/scheduler/` | SseHeartbeatScheduler |

**추가 의존성**: spring-boot-starter-web, actuator
**Flyway**: 의존성 자체를 exclude (DB 마이그레이션은 API에서만 실행)

```groovy
implementation(project(':devine-core')) {
    exclude group: 'org.flywaydb'
}
```

---

## 3. 의존성 그래프

```
devine-api ──────────→ devine-core ←──────────── devine-realtime
(bootJar)              (jar)                     (bootJar)
port 8080              라이브러리                  port 8081
                       bootJar 없음
```

- **core → api**: `implementation project(':devine-core')`
- **core → realtime**: `implementation project(':devine-core')` (Flyway exclude)
- **api ↔ realtime**: 직접 의존 없음. Redis Pub/Sub로만 통신

---

## 4. YAML 설정 전략

Spring Boot는 classpath의 `application.yml`을 하나만 로드하므로, core의 공유 설정은 `core-defaults.yml`로 분리하고 각 모듈에서 `spring.config.import`로 가져온다.

### 로딩 순서

```
.env (환경변수)  →  core-defaults.yml (공유)  →  application.yml (모듈)  →  application-{profile}.yml
```

```yaml
# devine-api/src/main/resources/application.yml
spring:
  config:
    import:
      - "optional:file:.env[.properties]"   # 1. 환경변수 로드
      - "classpath:core-defaults.yml"        # 2. 공유 설정 로드
```

### 파일별 역할

| 파일 | 위치 | 내용 |
|------|------|------|
| `core-defaults.yml` | devine-core | datasource, redis, flyway 기본값, JPA, OAuth2, redis channel |
| `application.yml` | devine-api | HikariCP 풀, S3, clerk, github, cors, server.port |
| `application.yml` | devine-realtime | HikariCP 풀, SSE timeout, executor, open-in-view: false |
| `application-dev.yml` | 각 모듈 | ddl-auto: validate, 디버그 로깅 |
| `application-prod.yml` | 각 모듈 | actuator 제한, 운영 로깅, executor 풀 확대, FastAPI 설정 |
| `application-test.yml` | 각 모듈 | server.port: 0, Testcontainers 연동 |

### 주의사항

- **동일 키 우선순위**: 모듈 yml > core-defaults.yml (나중에 로드된 것이 우선)
- **`.env` 파일 경로**: `bootRun`의 workingDir을 루트로 설정하여 `.env` 접근 보장
- **포트 변수 분리**: `${API_PORT:8080}` / `${REALTIME_PORT:8081}`

---

## 5. 핵심 변경 사항

### 5-1. 알림 이벤트 체인 변경

**변경 전** (단일 모듈, 같은 JVM):
```
NotificationCommandServiceImpl
  → ApplicationEventPublisher.publishEvent(NotificationCreatedEvent)
  → NotificationCreatedEventListener
  → SseEventPublisher
  → Redis publish
  → SseEventSubscriber (같은 JVM에서 수신)
  → SseEmitterManager → 클라이언트
```

**변경 후** (멀티모듈, 별도 JVM):
```
[devine-api]
NotificationCommandServiceImpl
  → TransactionSynchronization.afterCommit()
  → RedisTemplate.convertAndSend() (직접 publish)

[devine-realtime]
SseEventSubscriber (Redis MessageListener)
  → SseEmitterManager → 클라이언트
```

**핵심 코드** (`NotificationCommandServiceImpl.publishNotificationEvent`):
```java
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            SseEventPayload payload = SseEventPayload.builder()
                .eventId(String.valueOf(notification.getId()))
                .eventType(RedisEventConstants.NOTIFICATION)
                .receiverId(notification.getReceiver().getId())
                .data(detail)
                .build();
            String channel = notificationChannelPrefix + notification.getReceiver().getId();
            redisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(payload));
        }
    }
);
```

**삭제된 파일** (어느 모듈에도 이동하지 않음):
- `NotificationCreatedEvent.java`
- `NotificationCreatedEventListener.java`
- `SseEventPublisher.java`

### 5-2. SecurityConfig 분리

| 모듈 | 클래스 | 허용 엔드포인트 |
|------|--------|----------------|
| API | `ApiSecurityConfig` | `/api/**`, `/swagger-ui/**`, `/actuator/**` |
| Realtime | `RealtimeSecurityConfig` | `/sse/**`, `/actuator/**` |

두 모듈 모두 core의 `ClerkJwtAuthenticationConverter`를 공유하여 동일한 JWT 인증을 사용한다.

### 5-3. ExecutorConfig 분리

| 빈 | 모듈 | 용도 |
|----|------|------|
| `asyncTaskExecutor` | core (CoreExecutorConfig) | @Async 비동기 처리 |
| `sseConnectionExecutor` | realtime (RealtimeExecutorConfig) | SSE 연결 관리 |
| `sseDispatchExecutor` | realtime (RealtimeExecutorConfig) | SSE 이벤트 디스패치 |

### 5-4. RedisConfig 분리

| 모듈 | 내용 |
|------|------|
| core (`RedisConfig`) | `redisConnectionFactory`, `redisTemplate`, `objectRedisTemplate` |
| realtime (`RealtimeRedisConfig`) | `RedisMessageListenerContainer` + `SseEventSubscriber` 리스너 등록 |

### 5-5. 공유 상수/DTO

API(publish)와 Realtime(subscribe)이 동일한 계약에 의존하도록 core에 배치:

- `RedisEventConstants.NOTIFICATION` - 이벤트 타입 상수 (single source of truth)
- `SseEventPayload` - Redis Pub/Sub 메시지 DTO

---

## 6. 테스트 구조

### 클래스 계층

```
IntegrationTestSupport (testFixtures - 공유)
├── @SpringBootTest, @ActiveProfiles("test"), @Transactional
├── Testcontainers: PostgreSQL (pgvector:pg17), Valkey (Redis)
├── @DynamicPropertySource로 컨테이너 포트 주입
└── @AfterEach Redis 키 정리

CoreIntegrationTestSupport (core test)
├── extends IntegrationTestSupport
├── @SpringBootTest(classes = CoreTestApplication.class)
└── core 리포지토리 테스트용

ControllerIntegrationTestSupport (api test)
├── extends IntegrationTestSupport
├── @AutoConfigureMockMvc
└── MockMvc 주입, API 컨트롤러 테스트용
```

### CoreTestApplication

```java
@Configuration
@EnableAutoConfiguration
@ComponentScan("com.umc.devine")
@EnableJpaRepositories("com.umc.devine")
@EntityScan("com.umc.devine")
public class CoreTestApplication { }
```

- `@SpringBootApplication` 대신 `@Configuration` + `@EnableAutoConfiguration` 사용
- `@SpringBootConfiguration` 충돌 방지 (`DeVineApiApplication`과 동시 classpath 시)
- core의 **test** 소스에 위치 (testFixtures가 아님 - API 테스트에 노출 방지)

### java-test-fixtures 활용

```groovy
// devine-core/build.gradle
plugins { id 'java-test-fixtures' }

// devine-api/build.gradle
testImplementation testFixtures(project(':devine-core'))
```

`IntegrationTestSupport`를 `testFixtures`에 배치하여 API/Realtime 테스트에서 공유.
`CoreTestApplication`은 core의 일반 test 소스에 배치하여 다른 모듈에 노출되지 않음.

### 테스트 목록

**devine-core** (9개):
- `IntegrationTestSupportTest` - 테스트 환경 검증 (PostgreSQL, pgvector, Valkey 연결)
- `ImageRepositoryTest`, `ContactRepositoryTest`, `GitRepoUrlRepositoryTest`
- `MemberAgreementRepositoryTest`, `MemberRepositoryTest`, `TermsRepositoryTest`
- `ProjectRepositoryTest`, `DevReportRepositoryTest`

**devine-api** (11개):
- `DeVineApiApplicationTests` - 컨텍스트 로드 검증
- `MemberControllerTest`, `MyProfileControllerTest` - 컨트롤러 통합 테스트
- `MemberConverterTest` - 단위 테스트
- `MemberCommandServiceTest`, `MemberQueryServiceTest`, `ImageCommandServiceTest`
- `ProjectCommandServiceTest`, `ProjectQueryServiceTest`
- `ReportCommandServiceTechstackTest`, `ReportCommandServiceTransactionTest`

**devine-realtime**: 테스트 없음 (추후 추가)
