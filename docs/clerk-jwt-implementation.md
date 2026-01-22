# Clerk JWT 인증 구현 내역

## 개요

Spring Boot OAuth2 Resource Server를 활용하여 Clerk JWT를 검증하고, `@AuthenticationPrincipal ClerkPrincipal`로 사용자 정보에 접근할 수 있도록 구현했습니다.

## 아키텍처

```
[Client] → [Bearer Token] → [OAuth2 Resource Server] → [ClerkJwtAuthenticationConverter]
                                      ↓                            ↓
                              [Clerk JWKS로 검증]         [ClerkPrincipal 생성]
                                                                   ↓
                                              [@AuthenticationPrincipal로 주입]
```

## 구현 범위

| 항목 | 상태 |
|------|------|
| Clerk 토큰 검증 (OAuth2 Resource Server) | ✅ 완료 |
| ClerkPrincipal 객체로 clerkId, email 등 제공 | ✅ 완료 |
| Member 엔티티에 clerkId 필드 추가 | ✅ 완료 |
| 테스트용 `/api/v1/auth/me` 엔드포인트 | ✅ 완료 |
| 회원가입 플로우 | ❌ 다른 개발자 담당 |
| GitHub OAuth Token 조회 | ❌ 향후 구현 |

---

## 변경된 파일 목록

### 수정된 파일 (4개)

#### 1. `build.gradle`
```groovy
// 추가된 의존성
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

#### 2. `src/main/resources/application-dev.yml`
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${CLERK_ISSUER_URI}

clerk:
  secret-key: ${CLERK_SECRET_KEY}
```

#### 3. `src/main/java/com/umc/devine/domain/member/entity/Member.java`
```java
// 추가된 필드
@Column(name = "clerk_id", unique = true, length = 255)
private String clerkId;
```

#### 4. `src/main/java/com/umc/devine/domain/member/repository/MemberRepository.java`
```java
// 추가된 메서드
Optional<Member> findByClerkId(String clerkId);
boolean existsByClerkId(String clerkId);
```

---

### 새로 생성된 파일 (7개)

#### 인프라 계층 - `global/auth/`

| 파일 | 설명 |
|------|------|
| `ClerkPrincipal.java` | 인증된 사용자 정보 (clerkId, email, name, imageUrl) |
| `ClerkJwtAuthenticationConverter.java` | JWT → ClerkPrincipal 변환 |
| `CustomAuthenticationEntryPoint.java` | 인증 실패 시 JSON 응답 |

#### 비즈니스 계층 - `domain/auth/`

| 파일 | 설명 |
|------|------|
| `controller/AuthController.java` | `/api/v1/auth/me`, `/api/v1/auth/health` |
| `dto/AuthResDTO.java` | MeDTO, HealthDTO |
| `exception/code/AuthErrorCode.java` | UNAUTHORIZED, INVALID_TOKEN 등 |
| `exception/code/AuthSuccessCode.java` | HEALTH_OK, TOKEN_VALID |

---

## 파일 구조

```
src/main/java/com/umc/devine/
├── global/
│   ├── auth/                              ← 인프라 (Spring Security)
│   │   ├── ClerkPrincipal.java
│   │   ├── ClerkJwtAuthenticationConverter.java
│   │   └── CustomAuthenticationEntryPoint.java
│   └── config/
│       └── SecurityConfig.java            ← 수정됨
├── domain/
│   ├── auth/                              ← 비즈니스 (API)
│   │   ├── controller/AuthController.java
│   │   ├── dto/AuthResDTO.java
│   │   └── exception/code/
│   │       ├── AuthErrorCode.java
│   │       └── AuthSuccessCode.java
│   └── member/
│       ├── entity/Member.java             ← 수정됨
│       └── repository/MemberRepository.java ← 수정됨
```

---

## 환경 변수 설정

`.env` 파일에 다음 환경 변수를 추가해야 합니다:

```bash
# Clerk (Development)
CLERK_ISSUER_URI=https://<your-instance>.clerk.accounts.dev
CLERK_SECRET_KEY=sk_test_xxxxx
```

> **참고**: `CLERK_ISSUER_URI`는 Clerk 대시보드 > API Keys에서 확인할 수 있습니다.

---

## API 엔드포인트

### 1. Health Check
- **URL**: `GET /api/v1/auth/health`
- **인증**: 불필요
- **응답**:
```json
{
  "isSuccess": true,
  "code": "AUTH200_1",
  "message": "서버가 정상적으로 동작 중입니다.",
  "result": {
    "status": "UP",
    "message": "Auth service is running"
  }
}
```

### 2. 사용자 정보 조회
- **URL**: `GET /api/v1/auth/me`
- **인증**: Bearer Token 필요
- **응답**:
```json
{
  "isSuccess": true,
  "code": "AUTH200_2",
  "message": "사용자 정보를 성공적으로 조회했습니다.",
  "result": {
    "clerkId": "user_2abc123...",
    "email": "user@example.com",
    "memberId": null,
    "isRegistered": false
  }
}
```

---

## SecurityConfig 변경 내용

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", ...).permitAll()
            .requestMatchers("/api/v1/auth/health").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(clerkJwtAuthenticationConverter))
            .authenticationEntryPoint(customAuthenticationEntryPoint)
        )
        .build();
}
```

---

## 테스트 방법

### 1. Health Check (인증 불필요)
```bash
curl http://localhost:8080/api/v1/auth/health
```

### 2. 사용자 정보 조회 (인증 필요)
```bash
curl -H "Authorization: Bearer <CLERK_JWT>" \
     http://localhost:8080/api/v1/auth/me
```

---

## 주의사항

1. 기존 `global/jwt/` 패키지 코드는 건드리지 않음
2. 회원 자동 생성 로직 없음 (다른 개발자 담당)
3. `CLERK_ISSUER_URI` 환경변수 필수 설정
4. 모든 API 요청은 기본적으로 인증 필요 (permitAll 설정된 경로 제외)

---

## 작업 일자

- 2025-01-22

## 작성자
- sunm2n