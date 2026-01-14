# DeVine 통합 테스트 가이드

## 📚 목차
- [시작하기 전에](#시작하기-전에)
- [환경 설정](#환경-설정)
- [통합 테스트 작성 방법](#통합-테스트-작성-방법)
- [실행 및 확인](#실행-및-확인)
- [문제 해결](#문제-해결)

---

## 시작하기 전에

### 통합 테스트란?

우리 프로젝트는 **Testcontainers**를 사용하여 실제 운영 환경과 동일한 PostgreSQL 컨테이너에서 통합 테스트를 수행합니다.

**왜 Testcontainers를 사용하나요?**
- ✅ 운영과 동일한 PostgreSQL 17 + pgvector 환경에서 테스트
- ✅ H2 인메모리 DB로는 테스트할 수 없는 PostgreSQL 전용 기능 검증
- ✅ 개발자마다 환경이 달라서 생기는 "내 컴퓨터에서는 되는데요?" 문제 방지
- ✅ Docker만 있으면 누구나 동일한 환경에서 테스트 가능

---

## 환경 설정

### 1. Docker 설치 확인

Testcontainers는 Docker를 사용하므로 Docker Desktop이 설치되어 있어야 합니다.

```bash
# Docker 실행 확인
docker ps
```

만약 에러가 나면 [Docker Desktop](https://www.docker.com/products/docker-desktop/)을 설치하고 실행하세요.

---

### 2. Testcontainers Reuse 설정 (필수)

테스트 실행 속도를 크게 향상시키기 위해 컨테이너 재사용 기능을 활성화해야 합니다.

#### 설정 파일 생성

**방법 1: 터미널에서 직접 생성 (추천)**

```bash
# 파일 생성 및 내용 작성
cat > ~/.testcontainers.properties << 'EOF'
# Testcontainers 설정 파일
# 컨테이너 재사용 활성화 - 로컬 개발 시 테스트 실행 속도 향상
testcontainers.reuse.enable=true
EOF
```

**방법 2: 수동으로 생성**

1. 홈 디렉토리에 `.testcontainers.properties` 파일 생성
   - **Mac/Linux**: `/Users/사용자명/.testcontainers.properties`
   - **Windows**: `C:\Users\사용자명\.testcontainers.properties`

2. 파일 내용:
   ```properties
   # Testcontainers 설정 파일
   testcontainers.reuse.enable=true
   ```

#### 설정 확인

```bash
# 파일 생성 확인
cat ~/.testcontainers.properties
```

**출력 예시:**
```
testcontainers.reuse.enable=true
```

#### 효과

- **첫 테스트 실행**: ~10초 (컨테이너 시작)
- **두 번째 이후 실행**: ~2초 (기존 컨테이너 재사용)

**주의:** 이 설정 파일은 각 개발자가 **본인 PC에 직접 생성**해야 합니다. Git에는 포함되지 않습니다.

---

### 3. 프로젝트 의존성 확인

`build.gradle`에 이미 설정되어 있으니 별도 작업 불필요합니다:

```gradle
// Testcontainers
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:postgresql'
```

---

## 통합 테스트 작성 방법

### 기본 구조

모든 통합 테스트는 `IntegrationTestSupport` 클래스를 상속받아 작성합니다.

```java
package com.umc.devine.domain.member.repository;

import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("회원 Repository 테스트")
class MemberRepositoryTest extends IntegrationTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원을 저장하고 조회할 수 있다")
    void saveMember() {
        // given: 테스트 데이터 준비
        Member member = Member.builder()
            .name("홍길동")
            .nickname("gildong")
            .disclosure(true)
            .mainType(MemberMainType.DEVELOPER)
            .used(MemberStatus.ACTIVE)
            .build();

        // when: 저장
        Member savedMember = memberRepository.save(member);

        // then: 검증
        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("닉네임으로 회원을 조회할 수 있다")
    void findByNickname() {
        // given
        Member member = Member.builder()
            .name("김철수")
            .nickname("cheolsu")
            .disclosure(false)
            .mainType(MemberMainType.PM)
            .used(MemberStatus.ACTIVE)
            .build();
        memberRepository.save(member);

        // when
        Optional<Member> found = memberRepository.findByNickname("cheolsu");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("김철수");
    }
}
```

---

### IntegrationTestSupport가 제공하는 기능

#### 1. 자동 PostgreSQL 컨테이너 실행
```java
// IntegrationTestSupport를 상속받으면 자동으로:
// - pgvector/pgvector:pg17 이미지로 PostgreSQL 컨테이너 실행
// - Spring Boot가 자동으로 연결
// - 테스트 종료 후 자동 정리
```

#### 2. 테스트 격리 (@Transactional)
```java
@Test
void test1() {
    memberRepository.save(member1);
    // 테스트 종료 후 자동 롤백
}

@Test
void test2() {
    long count = memberRepository.count();
    // count == 0 (이전 테스트의 데이터가 롤백됨)
}
```

각 테스트는 완전히 독립적으로 실행되며, 테스트 순서에 상관없이 항상 동일한 결과를 보장합니다.

#### 3. 테스트 환경 설정
- `application-test.yml` 설정 자동 사용
- PostgreSQL, Redis 등 테스트용 설정 적용

---

### 작성 팁

#### ✅ DO - 이렇게 하세요

```java
// 1. DisplayName으로 명확한 테스트 의도 표현
@DisplayName("중복된 닉네임으로 회원가입 시 예외가 발생한다")

// 2. given-when-then 패턴 사용
@Test
void test() {
    // given: 테스트 데이터 준비

    // when: 실제 동작

    // then: 결과 검증
}

// 3. AssertJ 사용
assertThat(result).isNotNull();
assertThat(list).hasSize(2);
assertThat(member.getName()).isEqualTo("홍길동");
```

#### ❌ DON'T - 이렇게 하지 마세요

```java
// 1. 여러 테스트를 하나에 몰아넣기
@Test
void testEverything() {
    // 저장 테스트
    // 조회 테스트
    // 수정 테스트
    // 삭제 테스트 <- 분리하세요!
}

// 2. 테스트 간 의존성 만들기
@Test
@Order(1)
void createMember() { /* ... */ }

@Test
@Order(2)
void updateMember() { /* 위 테스트에 의존 <- 금지! */ }

// 3. Thread.sleep() 사용
@Test
void test() {
    Thread.sleep(1000); // ❌
    // @Transactional로 충분합니다
}
```

---

## 실행 및 확인

### 테스트 실행

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스만 실행
./gradlew test --tests MemberRepositoryTest

# 특정 테스트 메서드만 실행
./gradlew test --tests MemberRepositoryTest.saveMember
```

### 첫 실행 시

처음 실행하면 Docker가 `pgvector/pgvector:pg17` 이미지를 다운로드합니다:

```
> Task :test
Pulling image: pgvector/pgvector:pg17
Download complete
Creating container...
Container started in 2.5s
```

다운로드는 **처음 한 번만** 발생합니다.

### 로그 확인

테스트 실행 중 다음과 같은 로그를 볼 수 있습니다:

```
INFO tc.pgvector/pgvector:pg17 -- Creating container for image: pgvector/pgvector:pg17
INFO tc.pgvector/pgvector:pg17 -- Container started in PT0.746S
INFO tc.pgvector/pgvector:pg17 -- Container is started (JDBC URL: jdbc:postgresql://localhost:52162/testdb)
```

### 테스트 결과 확인

```bash
# 테스트 리포트 열기
open build/reports/tests/test/index.html
```

---

## 문제 해결

### 1. Docker 관련 에러

#### 증상
```
Could not find a valid Docker environment
```

#### 해결
1. Docker Desktop이 실행 중인지 확인
2. 터미널에서 `docker ps` 명령어 실행 확인

---

### 2. 포트 충돌 에러

#### 증상
```
Port already in use
```

#### 해결
실행 중인 Testcontainers 정리:
```bash
docker ps -a --filter "label=org.testcontainers=true" -q | xargs docker rm -f
```

---

### 3. 테스트 속도가 느림

#### 증상
매번 테스트 실행 시 10초 이상 소요

#### 원인
Testcontainers Reuse 설정이 안 됨

#### 해결
[2. Testcontainers Reuse 설정](#2-testcontainers-reuse-설정-필수) 섹션 참고

---

### 4. Redis 연결 에러

#### 증상
```
Port 0 must be a valid TCP port
```

#### 해결
이미 `application-test.yml`에 설정되어 있어야 합니다:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

만약 없다면 추가하세요.

---

### 5. 컨테이너가 계속 쌓임

#### 증상
Docker Desktop에서 testcontainers 컨테이너가 많이 쌓여있음

#### 해결
```bash
# Reuse 설정 확인
cat ~/.testcontainers.properties

# 출력에 다음이 포함되어야 함:
# testcontainers.reuse.enable=true

# 모든 testcontainers 정리
docker ps -a --filter "label=org.testcontainers=true" -q | xargs docker rm -f
```

---

### 6. 이미지 다운로드 실패

#### 증상
```
Unable to pull image: pgvector/pgvector:pg17
```

#### 해결
```bash
# 수동으로 이미지 다운로드
docker pull pgvector/pgvector:pg17

# 네트워크 연결 확인
# VPN이나 회사 방화벽 때문일 수 있습니다
```

---

## 참고 자료

### 프로젝트 구조
```
src/
├── main/
│   └── java/com/umc/devine/
│       └── domain/member/
│           ├── entity/Member.java
│           └── repository/MemberRepository.java
└── test/
    └── java/com/umc/devine/
        ├── support/                              ← 테스트 인프라
        │   ├── IntegrationTestSupport.java      (베이스 클래스)
        │   └── IntegrationTestSupportTest.java  (검증 테스트)
        └── domain/member/
            └── repository/
                └── MemberRepositoryTest.java     ← 실제 테스트
```

### IntegrationTestSupport 내부 동작

```java
@SpringBootTest              // 스프링 부트 통합 테스트 환경
@Testcontainers             // Testcontainers 활성화
@ActiveProfiles("test")     // application-test.yml 사용
@Transactional              // 각 테스트 메서드 후 자동 롤백
public abstract class IntegrationTestSupport {

    @Container
    @ServiceConnection      // Spring Boot가 자동으로 연결 정보 주입
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
        new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);  // 컨테이너 재사용
}
```

---

## 체크리스트

테스트 작성 전 확인사항:

- [ ] Docker Desktop 설치 및 실행 확인
- [ ] `~/.testcontainers.properties` 파일 생성 확인
- [ ] `testcontainers.reuse.enable=true` 설정 확인
- [ ] 프로젝트 의존성 확인 (`build.gradle`)
- [ ] `IntegrationTestSupport` 클래스 존재 확인

첫 테스트 작성 시:

- [ ] `IntegrationTestSupport` 상속
- [ ] `@DisplayName` 작성
- [ ] given-when-then 패턴 사용
- [ ] 각 테스트는 독립적으로 작성
- [ ] AssertJ 사용하여 검증

---

## 추가 도움이 필요하면?

1. **IntegrationTestSupportTest 실행**: 환경이 제대로 설정되었는지 확인
   ```bash
   ./gradlew test --tests IntegrationTestSupportTest
   ```

2. **로그 확인**: 테스트 실행 시 Testcontainers 로그 확인

3. **팀원에게 문의**: 이미 설정한 팀원에게 도움 요청

---

**문서 작성일**: 2026-01-14
**작성자**: DeVine Backend Team
