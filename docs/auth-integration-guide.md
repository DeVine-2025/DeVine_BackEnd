# 인증 연동 가이드

다른 도메인에서 Clerk JWT 인증을 연동하는 방법을 설명합니다.

---

## 빠른 시작

### 1. 컨트롤러에서 인증된 사용자 정보 받기

```java
import com.umc.devine.global.auth.ClerkPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @GetMapping("/my")
    public ApiResponse<ProjectListDTO> getMyProjects(
            @AuthenticationPrincipal ClerkPrincipal principal
    ) {
        String clerkId = principal.getClerkId();
        String email = principal.getEmail();

        // clerkId로 Member 조회
        Member member = memberRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.NOT_REGISTERED));

        // 비즈니스 로직 수행
        return ApiResponse.onSuccess(code, projectService.getMyProjects(member.getId()));
    }
}
```

---

## ClerkPrincipal 구조

`@AuthenticationPrincipal ClerkPrincipal`로 주입받을 수 있는 사용자 정보:

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `clerkId` | String | Clerk 사용자 고유 ID | `user_2abc123xyz` |
| `email` | String | 사용자 이메일 | `user@example.com` |
| `name` | String | 사용자 이름 (nullable) | `홍길동` |
| `imageUrl` | String | 프로필 이미지 URL (nullable) | `https://...` |

```java
// 사용 예시
@GetMapping("/profile")
public ApiResponse<ProfileDTO> getProfile(@AuthenticationPrincipal ClerkPrincipal principal) {
    String clerkId = principal.getClerkId();    // 필수
    String email = principal.getEmail();        // 필수
    String name = principal.getName();          // nullable
    String imageUrl = principal.getImageUrl();  // nullable

    // ...
}
```

---

## 일반적인 사용 패턴

### 패턴 1: clerkId로 Member 조회 (가입된 사용자만)

```java
@GetMapping("/dashboard")
public ApiResponse<DashboardDTO> getDashboard(
        @AuthenticationPrincipal ClerkPrincipal principal
) {
    // Member 조회 - 없으면 예외 발생
    Member member = memberRepository.findByClerkId(principal.getClerkId())
            .orElseThrow(() -> new CustomException(AuthErrorCode.NOT_REGISTERED));

    return ApiResponse.onSuccess(code, dashboardService.get(member));
}
```

### 패턴 2: 가입 여부 확인 후 분기 처리

```java
@GetMapping("/status")
public ApiResponse<StatusDTO> getStatus(
        @AuthenticationPrincipal ClerkPrincipal principal
) {
    Optional<Member> memberOpt = memberRepository.findByClerkId(principal.getClerkId());

    if (memberOpt.isPresent()) {
        // 가입된 사용자
        return ApiResponse.onSuccess(code, StatusDTO.registered(memberOpt.get()));
    } else {
        // 미가입 사용자 (Clerk 인증은 됨)
        return ApiResponse.onSuccess(code, StatusDTO.notRegistered(principal.getEmail()));
    }
}
```

### 패턴 3: memberId 추출 헬퍼 메서드 활용

서비스 클래스에서 공통으로 사용할 헬퍼 메서드를 만들 수 있습니다:

```java
@Service
@RequiredArgsConstructor
public class AuthHelper {

    private final MemberRepository memberRepository;

    /**
     * ClerkPrincipal에서 Member 엔티티 조회
     * @throws CustomException 미가입 사용자인 경우
     */
    public Member getMember(ClerkPrincipal principal) {
        return memberRepository.findByClerkId(principal.getClerkId())
                .orElseThrow(() -> new CustomException(AuthErrorCode.NOT_REGISTERED));
    }

    /**
     * ClerkPrincipal에서 memberId 추출
     * @throws CustomException 미가입 사용자인 경우
     */
    public Long getMemberId(ClerkPrincipal principal) {
        return getMember(principal).getId();
    }

    /**
     * 가입 여부 확인
     */
    public boolean isRegistered(ClerkPrincipal principal) {
        return memberRepository.existsByClerkId(principal.getClerkId());
    }
}
```

사용 예시:
```java
@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final AuthHelper authHelper;
    private final BookmarkService bookmarkService;

    @PostMapping("/bookmarks")
    public ApiResponse<BookmarkDTO> createBookmark(
            @AuthenticationPrincipal ClerkPrincipal principal,
            @RequestBody BookmarkReqDTO request
    ) {
        Long memberId = authHelper.getMemberId(principal);
        return ApiResponse.onSuccess(code, bookmarkService.create(memberId, request));
    }
}
```

---

## MemberRepository 메서드

Member 조회에 사용할 수 있는 메서드:

```java
public interface MemberRepository extends JpaRepository<Member, Long> {

    // clerkId로 Member 조회
    Optional<Member> findByClerkId(String clerkId);

    // clerkId 존재 여부 확인
    boolean existsByClerkId(String clerkId);
}
```

---

## 인증 실패 응답

인증이 필요한 API에 토큰 없이 접근하면 다음 응답이 반환됩니다:

```json
{
  "isSuccess": false,
  "code": "AUTH401_1",
  "message": "인증이 필요합니다.",
  "result": null
}
```

HTTP Status: `401 Unauthorized`

---

## 인증이 필요 없는 API 만들기

특정 엔드포인트를 인증 없이 접근 가능하게 하려면 `SecurityConfig`를 수정해야 합니다:

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/api/v1/auth/health").permitAll()
    // 새로운 공개 API 추가
    .requestMatchers("/api/v1/public/**").permitAll()
    .anyRequest().authenticated()
)
```

---

## 에러 코드

### AuthErrorCode

| 코드 | HTTP Status | 설명 |
|------|-------------|------|
| `AUTH401_1` | 401 | 인증이 필요합니다 |
| `AUTH401_2` | 401 | 유효하지 않은 토큰입니다 |
| `AUTH401_3` | 401 | 만료된 토큰입니다 |
| `AUTH403_1` | 403 | 가입되지 않은 사용자입니다 |

사용 예시:
```java
import com.umc.devine.domain.auth.exception.code.AuthErrorCode;

throw new CustomException(AuthErrorCode.NOT_REGISTERED);
```

---

## 테스트 방법

### Postman / curl 테스트

```bash
# 인증이 필요한 API 호출
curl -X GET http://localhost:8080/api/v1/projects/my \
  -H "Authorization: Bearer <CLERK_JWT_TOKEN>"
```

### Clerk JWT 토큰 얻는 방법

1. **프론트엔드에서**: Clerk SDK의 `getToken()` 메서드 사용
   ```javascript
   const token = await clerk.session.getToken();
   ```

2. **테스트용**: Clerk 대시보드 > Sessions에서 토큰 복사

---

## 자주 묻는 질문

### Q: Principal이 null로 들어와요
**A**:
1. `Authorization: Bearer <token>` 헤더가 정확히 설정되었는지 확인
2. 토큰이 만료되지 않았는지 확인
3. `CLERK_ISSUER_URI` 환경변수가 올바르게 설정되었는지 확인

### Q: 미가입 사용자도 API를 호출할 수 있나요?
**A**: 네, Clerk JWT 인증만 통과하면 API 호출이 가능합니다. 하지만 `memberRepository.findByClerkId()`가 빈 Optional을 반환하므로, 가입된 사용자만 허용하려면 예외를 던지세요.

### Q: 특정 API만 가입된 사용자로 제한하고 싶어요
**A**: 컨트롤러나 서비스에서 가입 여부를 확인하고 예외를 던지면 됩니다:
```java
if (!memberRepository.existsByClerkId(principal.getClerkId())) {
    throw new CustomException(AuthErrorCode.NOT_REGISTERED);
}
```

### Q: clerkId와 memberId의 차이가 뭔가요?
**A**:
- `clerkId`: Clerk에서 발급하는 사용자 고유 ID (예: `user_2abc123xyz`)
- `memberId`: 우리 DB의 Member 테이블 PK (예: `1`, `2`, `3`)

Clerk 인증만 되고 회원가입을 안 한 사용자는 `clerkId`는 있지만 `memberId`는 없습니다.

---

## 기존 코드 마이그레이션

기존에 하드코딩된 `memberId`를 사용하던 코드를 마이그레이션하는 방법:

### Before
```java
@GetMapping("/")
public ApiResponse<MemberDetailDTO> getMember() {
    // TODO: 토큰 방식으로 변경
    Long memberId = 1L;  // 하드코딩

    return ApiResponse.onSuccess(code, memberQueryService.findMemberById(memberId));
}
```

### After
```java
@GetMapping("/")
public ApiResponse<MemberDetailDTO> getMember(
        @AuthenticationPrincipal ClerkPrincipal principal
) {
    Member member = memberRepository.findByClerkId(principal.getClerkId())
            .orElseThrow(() -> new CustomException(AuthErrorCode.NOT_REGISTERED));

    return ApiResponse.onSuccess(code, memberQueryService.findMemberById(member.getId()));
}
```

---

## 참고 자료

- [Clerk 공식 문서](https://clerk.com/docs)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- 구현 상세: `docs/clerk-jwt-implementation.md`


## 작성자

- sunm2n