# open-in-view=false 전환 가이드

## 1. 배경

`divine-api`는 `spring.jpa.open-in-view` 설정이 없어 Spring Boot 기본값인 `true`가 적용된 상태다.
OSIV(Open Session In View)=true일 때 HTTP 요청 전체 라이프사이클 동안 DB 커넥션을 점유하기 때문에, 트래픽이 몰리면 HikariCP 커넥션 풀이 고갈된다.

참고: `devine-realtime`은 이미 `open-in-view: false`가 적용되어 있다.

---

## 2. OSIV가 뭔지

OSIV=true이면 Spring이 서블릿 필터 레벨에서 JPA 영속성 컨텍스트(Persistence Context)를 열고, HTTP 응답이 나갈 때까지 유지한다. 그 사이에 DB 커넥션도 계속 점유된다.

```
OSIV=true
─────────────────────────────────────────────────────
[Filter] PC 오픈 + DB 커넥션 획득
  → ArgumentResolver
  → Controller
  → @Transactional Service   ← 기존 PC에 합류
  → Controller (응답 직렬화)  ← 여기서도 LAZY 로드 가능
[Filter] PC 닫기 + DB 커넥션 반납
─────────────────────────────────────────────────────

OSIV=false
─────────────────────────────────────────────────────
[Filter] PC 없음
  → ArgumentResolver
  → Controller
  → @Transactional Service   ← 새 PC + DB 커넥션 획득
                              ← 트랜잭션 종료 시 즉시 반납
  → Controller (응답 직렬화)  ← PC 없음, LAZY 로드 불가
─────────────────────────────────────────────────────
```

---

## 3. CurrentMemberArgumentResolver 문제

이 프로젝트에서 OSIV 전환 시 가장 주의해야 할 지점은 `CurrentMemberArgumentResolver`다.

```java
// CurrentMemberArgumentResolver.java
@Override
public Object resolveArgument(...) {
    // 이 시점에 트랜잭션이 없다.
    // Spring Data가 쿼리용 트랜잭션을 잠깐 열었다가 즉시 닫는다.
    return memberRepository.findByClerkId(clerkPrincipal.getClerkId())
            .orElseThrow(() -> new AuthException(AuthErrorReason.NOT_REGISTERED));
}
// → 반환된 Member 엔티티는 이 시점부터 DETACHED 상태
```

OSIV=true일 때는 ArgumentResolver와 Service가 같은 영속성 컨텍스트를 공유하므로 Member가 MANAGED 상태를 유지했다.
OSIV=false로 바꾸면 @Transactional Service 메서드가 시작될 때 새 영속성 컨텍스트가 만들어지고, 여기에 Member는 속해 있지 않아 DETACHED 상태가 된다.

**DETACHED 엔티티의 문제점:**
- setter 호출해도 dirty tracking이 동작하지 않아 변경사항이 DB에 반영되지 않음
- LAZY 컬렉션 접근 시 `LazyInitializationException` 발생

---

## 4. 영향 범위

### 즉각 오류 발생 (필수 수정)

#### `MemberCommandServiceImpl.updateMember()`

```java
// 문제: member는 DETACHED 상태
public MemberResDTO.MemberProfileDTO updateMember(Member member, UpdateMemberDTO dto) {
    member.updateNickname(dto.nickname());  // dirty tracking 안 됨 → DB 미반영
    // ...
    member.clearCategories();               // memberCategories는 LAZY 컬렉션
                                            // → LazyInitializationException!
    entityManager.flush();
    member.addCategories(categories);
    // ...
}
```

#### `MemberCommandServiceImpl.withdraw()`

```java
// 문제: member.withdraw()가 DB에 반영되지 않음
public void withdraw(Member member) {
    member.withdraw();  // dirty tracking 안 됨 → 탈퇴 처리 무효
    // ...
}
```

### 동작하지만 검토 필요

| 메서드 | 상태 | 이유 |
|--------|------|------|
| `MemberQueryServiceImpl.findMyContributions()` | 확인 필요 | githubUsername 없을 때 `member.updateGithubUsername()` 호출 가능성 |
| `MemberCommandServiceImpl.addMemberTechstacks()` | 안전 | `member`를 쿼리 파라미터로만 사용, Spring Data가 ID 기반 처리 |
| `MemberCommandServiceImpl.removeMemberTechstacks()` | 안전 | 동일 |
| `MemberCommandServiceImpl.syncGitHubRepositories()` | 확인 필요 | member setter 호출 여부 확인 |

### 안전한 영역

- **모든 QueryServiceImpl**: 클래스 레벨 `@Transactional(readOnly = true)` 적용, 도메인 엔티티를 Service 내에서 직접 로드 → MANAGED
- **ProjectCommandServiceImpl, MatchingCommandServiceImpl, BookmarkCommandServiceImpl**: `Member` 파라미터를 FK 참조나 쿼리 파라미터로만 사용, JPA가 ID 기반으로 처리 → OK
- **PaymentCommandServiceImpl**: `TransactionTemplate`으로 명시적 트랜잭션 관리, DTO 변환을 람다 안에서 수행 → OK
- **Controller 레이어**: 모든 Controller가 DTO를 반환, entity를 직접 직렬화하지 않음 → OK

---

## 5. 수정 방법

DETACHED 엔티티를 받는 메서드에서 진입 직후 re-fetch한다.

```java
// Before
@Override
public MemberResDTO.MemberProfileDTO updateMember(Member member, MemberReqDTO.UpdateMemberDTO dto) {
    if (dto.nickname() != null && !dto.nickname().equals(member.getNickname())) {
        // ...
    }
    member.updateNickname(dto.nickname());  // dirty tracking 안 됨!
    member.clearCategories();               // LazyInitializationException!
    // ...
}

// After
@Override
public MemberResDTO.MemberProfileDTO updateMember(Member member, MemberReqDTO.UpdateMemberDTO dto) {
    Member managed = memberRepository.findById(member.getId())
            .orElseThrow(() -> new MemberException(MemberErrorReason.MEMBER_NOT_FOUND));

    if (dto.nickname() != null && !dto.nickname().equals(managed.getNickname())) {
        // ...
    }
    managed.updateNickname(dto.nickname());  // MANAGED → dirty tracking 작동
    managed.clearCategories();               // LAZY 컬렉션 접근 가능
    // ...
}
```

---

## 6. 수정 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `devine-api/src/main/resources/application.yml` | `spring.jpa.open-in-view: false` 추가 |
| `devine-api/.../member/service/command/MemberCommandServiceImpl.java` | `updateMember()`, `withdraw()` 상단에 managed member re-fetch 추가 |
| `devine-api/.../member/service/query/MemberQueryServiceImpl.java` | `findMyContributions()` 확인 후 필요 시 re-fetch 추가 |

---

## 7. 검증 방법

1. 앱 기동 로그에서 `spring.jpa.open-in-view is enabled by default` 경고 메시지 사라지는지 확인
2. 회원 프로필 수정 API (`PATCH /api/v1/members`) 호출 → 닉네임, 카테고리 정상 변경 확인
3. 회원 탈퇴 API 흐름 확인
4. 기존 테스트 전체 실행: `./gradlew :devine-api:test`
5. HikariCP 커넥션 모니터링: `hikaricp.connections.active` 지표 확인