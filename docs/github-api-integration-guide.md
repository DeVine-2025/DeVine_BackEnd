# GitHub API 연동 가이드

다른 도메인에서 GitHub API를 쉽게 사용하는 방법을 설명합니다.

---

## 개요

### 아키텍처

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  다른 도메인     │     │  GitHubService  │     │   외부 API      │
│  (Member 등)    │────▶│  (퍼사드)        │────▶│                 │
└─────────────────┘     └─────────────────┘     │  - Clerk API    │
                               │                │  - GitHub API   │
                               ▼                └─────────────────┘
                        clerkId만 전달하면
                        토큰 조회 + API 호출 자동 처리
```

### 핵심 포인트

- **토큰 관리 불필요**: `clerkId`만 전달하면 내부적으로 Clerk에서 GitHub 토큰을 조회
- **보안**: 토큰은 캐싱하지 않고 매번 실시간 조회 (토큰 노출 위험 최소화)
- **타입 안전**: `Map<String, Object>` 대신 전용 DTO 사용

### 두 가지 사용 방식

| 구분 | 메서드 | 토큰 | 용도 |
|------|--------|------|------|
| **내 데이터 조회** | `getContributions(clerkId)` | 사용자 토큰 (Clerk) | 내 잔디, 내 레포 조회 |
| **다른 사람 조회** | `getContributionsByUsername(username)` | 서비스 토큰 (환경변수) | 다른 회원 잔디 조회 |

---

## 빠른 시작

### 1. GitHubService 주입

```java
import com.umc.devine.global.external.github.GitHubService;
import com.umc.devine.global.external.github.dto.GitHubContributionDTO;
import com.umc.devine.global.external.github.dto.GitHubRepositoryDTO;

@Service
@RequiredArgsConstructor
public class MemberQueryServiceImpl implements MemberQueryService {

    private final GitHubService gitHubService;
    private final MemberRepository memberRepository;

    // ...
}
```

### 2. 기본 사용법

```java
public ContributionListDTO findContributionsById(Long memberId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

    // clerkId만 전달하면 됨!
    List<GitHubContributionDTO> contributions =
        gitHubService.getContributions(member.getClerkId());

    // DTO 변환
    return convertToContributionListDTO(contributions);
}
```

---

## 사용 가능한 메서드

### GitHubService

#### 사용자 토큰 사용 (내 데이터 조회)

| 메서드 | 설명 | 파라미터 |
|--------|------|----------|
| `getRepositories(clerkId)` | 내 레포지토리 목록 조회 | clerkId |
| `getContributions(clerkId)` | 내 잔디 데이터 조회 | clerkId |
| `getUserInfo(clerkId)` | 내 GitHub 사용자 정보 조회 | clerkId |

#### 서비스 토큰 사용 (다른 사용자 조회)

| 메서드 | 설명 | 파라미터 |
|--------|------|----------|
| `getContributionsByUsername(username)` | 다른 사용자의 잔디 조회 | GitHub 사용자명 |

> **참고**: `getContributionsByUsername`은 서비스 토큰(`GITHUB_SERVICE_TOKEN`)이 설정되어 있어야 동작합니다. 토큰이 없으면 빈 목록을 반환합니다.

---

## DTO 구조

### GitHubRepositoryDTO

레포지토리 정보를 담는 DTO입니다.

```java
public class GitHubRepositoryDTO {
    private String name;        // 레포지토리 이름
    private String description; // 레포지토리 설명
    private String htmlUrl;     // GitHub URL (https://github.com/...)
}
```

**사용 예시:**
```java
List<GitHubRepositoryDTO> repos = gitHubService.getRepositories(clerkId);

for (GitHubRepositoryDTO repo : repos) {
    System.out.println("이름: " + repo.getName());
    System.out.println("설명: " + repo.getDescription());
    System.out.println("URL: " + repo.getHtmlUrl());
}
```

### GitHubContributionDTO

잔디(기여) 데이터를 담는 DTO입니다.

```java
public class GitHubContributionDTO {
    private String date;             // 날짜 (YYYY-MM-DD 형식)
    private Integer contributionCount; // 해당 날짜의 기여 수
}
```

**사용 예시:**
```java
List<GitHubContributionDTO> contributions = gitHubService.getContributions(clerkId);

for (GitHubContributionDTO contribution : contributions) {
    System.out.println(contribution.getDate() + ": " + contribution.getContributionCount() + "개");
}
// 출력 예: 2024-01-15: 5개
```

---

## 상세 사용 예시

### 레포지토리 목록 조회

```java
@Service
@RequiredArgsConstructor
public class ReportCommandServiceImpl implements ReportCommandService {

    private final GitHubService gitHubService;
    private final MemberRepository memberRepository;

    @Override
    public RepositoryListDTO getRepositoriesForReport(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

        List<GitHubRepositoryDTO> repos = gitHubService.getRepositories(member.getClerkId());

        // 응답 DTO로 변환
        List<RepositoryDTO> repoList = repos.stream()
                .map(repo -> RepositoryDTO.builder()
                        .name(repo.getName())
                        .description(repo.getDescription())
                        .url(repo.getHtmlUrl())
                        .build())
                .collect(Collectors.toList());

        return RepositoryListDTO.builder()
                .repositories(repoList)
                .build();
    }
}
```

### 잔디(Contributions) 조회

```java
@Override
public ContributionListDTO findContributionsById(Long memberId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

    List<GitHubContributionDTO> contributions =
        gitHubService.getContributions(member.getClerkId());

    // MemberResDTO.ContributionDTO로 변환
    List<MemberResDTO.ContributionDTO> contributionList = contributions.stream()
            .map(c -> MemberResDTO.ContributionDTO.builder()
                    .date(c.getDate())
                    .count(c.getContributionCount())
                    .build())
            .collect(Collectors.toList());

    return MemberResDTO.ContributionListDTO.builder()
            .contributionList(contributionList)
            .build();
}
```

### GitHub 사용자 정보 조회

```java
Map<String, Object> userInfo = gitHubService.getUserInfo(clerkId);

String login = (String) userInfo.get("login");      // GitHub 사용자명
String name = (String) userInfo.get("name");        // 실명
String avatarUrl = (String) userInfo.get("avatar_url"); // 프로필 이미지
String bio = (String) userInfo.get("bio");          // 자기소개
```

---

## 예외 처리

### 주의사항: Google 로그인 사용자

> **중요**: Google 로그인 사용자는 GitHub 연동이 없으므로 `AuthException`이 발생합니다.

GitHub API를 사용할 때는 반드시 예외 처리를 해야 합니다:

```java
import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorCode;

@Override
public ContributionListDTO findContributionsById(Long memberId) {
    Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

    try {
        List<GitHubContributionDTO> contributions =
            gitHubService.getContributions(member.getClerkId());

        // 정상 처리
        return convertToContributionListDTO(contributions);

    } catch (AuthException e) {
        if (e.getErrorCode() == AuthErrorCode.GITHUB_TOKEN_NOT_FOUND) {
            // GitHub 연동이 없는 사용자 (Google 로그인 등)
            // 빈 목록 반환 또는 적절한 처리
            return ContributionListDTO.builder()
                    .contributionList(Collections.emptyList())
                    .build();
        }
        // 그 외 예외는 재던지기
        throw e;
    }
}
```

### 에러 코드

| 코드 | HTTP Status | 설명 | 원인 |
|------|-------------|------|------|
| `AUTH404_1` | 404 | GitHub 연동 정보를 찾을 수 없습니다 | Google 로그인 사용자 등 |
| `AUTH404_2` | 404 | 존재하지 않는 GitHub 사용자입니다 | 잘못된 username 조회 |
| `AUTH500_1` | 500 | GitHub 토큰 조회에 실패했습니다 | Clerk API 통신 오류 |
| `AUTH502_1` | 502 | Clerk API 호출에 실패했습니다 | Clerk 서버 오류 |
| `AUTH502_2` | 502 | GitHub API 호출에 실패했습니다 | GitHub 서버 오류 |

### 예외별 처리 가이드

```java
try {
    List<GitHubRepositoryDTO> repos = gitHubService.getRepositories(clerkId);
    // ...
} catch (AuthException e) {
    switch (e.getErrorCode()) {
        case GITHUB_TOKEN_NOT_FOUND:
            // GitHub 연동 없음 → 빈 목록 또는 연동 유도 메시지
            return emptyResult();

        case GITHUB_USER_NOT_FOUND:
            // 존재하지 않는 GitHub 사용자 → 빈 목록 또는 에러 응답
            return emptyResult();

        case GITHUB_TOKEN_FETCH_FAILED:
        case CLERK_API_ERROR:
            // Clerk 통신 오류 → 재시도 또는 에러 응답
            throw new ServiceException("일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");

        case GITHUB_API_ERROR:
            // GitHub API 오류 → 재시도 또는 에러 응답
            throw new ServiceException("GitHub 서비스에 일시적인 문제가 있습니다.");

        default:
            throw e;
    }
}
```

---

## 내부 동작 원리

### 토큰 흐름

```
1. GitHubService.getRepositories(clerkId) 호출
                    │
                    ▼
2. ClerkApiClient.getGitHubAccessToken(clerkId)
   → Clerk API 호출: GET /v1/users/{clerkId}/oauth_access_tokens/oauth_github
   → GitHub Access Token 반환
                    │
                    ▼
3. GitHubApiClient.getRepositories(accessToken)
   → GitHub API 호출: GET /user/repos
   → 레포지토리 목록 반환
```

### 보안 고려사항

- **토큰 캐싱 없음**: OAuth 토큰은 매번 Clerk에서 실시간 조회
- **토큰 노출 최소화**: 토큰은 `global/external` 레이어 내부에서만 사용
- **다른 도메인은 clerkId만 알면 됨**: 토큰을 직접 다룰 필요 없음

---

## 파일 위치

```
global/external/
├── clerk/
│   ├── ClerkApiClient.java          # Clerk API 호출 클라이언트
│   └── dto/
│       └── ClerkOAuthTokenResponse.java
└── github/
    ├── GitHubApiClient.java         # GitHub API 호출 클라이언트 (Low-level)
    ├── GitHubService.java           # GitHub 통합 서비스 (퍼사드) ← 이것만 사용!
    └── dto/
        ├── GitHubContributionDTO.java
        └── GitHubRepositoryDTO.java
```

**다른 도메인에서는 `GitHubService`만 주입받아 사용하면 됩니다.**

---

## 자주 묻는 질문

### Q: GitHubApiClient와 GitHubService의 차이점은?

**A**:
- `GitHubApiClient`: Low-level 클라이언트. accessToken을 직접 전달해야 함
- `GitHubService`: 퍼사드. clerkId만 전달하면 토큰 조회를 자동으로 처리

**항상 `GitHubService`를 사용하세요.**

### Q: GitHub 연동이 없는 사용자를 어떻게 구분하나요?

**A**: `AuthException`의 에러 코드를 확인하세요:
```java
catch (AuthException e) {
    if (e.getErrorCode() == AuthErrorCode.GITHUB_TOKEN_NOT_FOUND) {
        // GitHub 연동 없음
    }
}
```

### Q: 레포지토리가 너무 많으면 어떻게 되나요?

**A**: 현재 GitHub API의 기본 페이지네이션(최대 30개)이 적용됩니다. 더 많은 레포지토리가 필요하면 `GitHubApiClient`에 페이지네이션 파라미터를 추가해야 합니다.

### Q: 잔디 데이터는 어느 기간의 데이터인가요?

**A**: GitHub GraphQL API의 `contributionCalendar`는 최근 1년간의 데이터를 반환합니다.

### Q: private 레포지토리도 조회되나요?

**A**: Clerk에서 설정한 GitHub OAuth 스코프에 따라 다릅니다. 현재 설정으로는 public 레포지토리만 조회됩니다.

### Q: API 호출 횟수 제한이 있나요?

**A**:
- GitHub REST API: 인증된 요청 기준 시간당 5,000회
- GitHub GraphQL API: 시간당 5,000 포인트

일반적인 사용에서는 제한에 도달하기 어렵습니다.

---

## 다른 사용자 잔디 조회

다른 사용자의 잔디를 조회할 때는 **서비스 토큰**을 사용합니다.

### 서비스 토큰 방식

```java
// 다른 사용자의 잔디 조회 (로그인 불필요)
public ContributionListDTO findContributionsByNickname(String nickname) {
    Member target = memberRepository.findByNickname(nickname)
            .orElseThrow(() -> new MemberException(MemberErrorCode.NOT_FOUND));

    // githubUsername이 없으면 빈 목록
    if (target.getGithubUsername() == null) {
        return emptyContributions();
    }

    // 서비스 토큰으로 조회 (환경변수: GITHUB_SERVICE_TOKEN)
    List<GitHubContributionDTO> contributions =
        gitHubService.getContributionsByUsername(target.getGithubUsername());

    return convertToContributionListDTO(contributions);
}
```

### 환경변수 설정

```bash
# .env
GITHUB_SERVICE_TOKEN=ghp_xxxxxxxxxxxx
```

GitHub Personal Access Token 생성:
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate new token → Expiration: "No expiration" 선택
3. Scope: `read:user` 체크

---

## 회원가입 시 githubUsername 저장

GitHub 로그인 사용자의 경우, 회원가입 시 `githubUsername`을 저장해야 합니다.

### Member 엔티티

```java
// Member.java
@Column(name = "github_username", nullable = true, length = 39)
private String githubUsername;  // GitHub 로그인: 저장, Google 로그인: null
```

### 회원가입 로직 (구현 필요)

```java
// 회원가입 시 GitHub 사용자인 경우 username 저장
public Member createMember(CreateMemberRequest request, String clerkId) {
    String githubUsername = null;

    // GitHub 로그인 사용자인 경우 username 조회
    try {
        Map<String, Object> userInfo = gitHubService.getUserInfo(clerkId);
        githubUsername = (String) userInfo.get("login");
    } catch (AuthException e) {
        // Google 로그인 등 GitHub 연동이 없는 경우 무시
    }

    Member member = Member.builder()
            .clerkId(clerkId)
            .name(request.getName())
            .nickname(request.getNickname())
            .githubUsername(githubUsername)  // GitHub 사용자만 저장됨
            // ...
            .build();

    return memberRepository.save(member);
}
```

---

## 체크리스트

### 내 데이터 조회 시

- [ ] `GitHubService`를 주입받았는가?
- [ ] `clerkId`를 Member 엔티티에서 가져오고 있는가?
- [ ] `AuthException` 예외 처리를 했는가? (특히 `GITHUB_TOKEN_NOT_FOUND`)
- [ ] 응답 DTO 변환 로직을 작성했는가?

### 다른 사용자 잔디 조회 시

- [ ] `GITHUB_SERVICE_TOKEN` 환경변수가 설정되어 있는가?
- [ ] 대상 Member의 `githubUsername` 필드를 확인하는가?
- [ ] `githubUsername`이 null인 경우 빈 목록을 반환하는가?
- [ ] 회원가입 시 GitHub 사용자의 `githubUsername`을 저장하는가?

---

## 참고 자료

- [GitHub REST API 문서](https://docs.github.com/en/rest)
- [GitHub GraphQL API 문서](https://docs.github.com/en/graphql)
- [Clerk OAuth 문서](https://clerk.com/docs/authentication/social-connections/oauth)
- 인증 연동 가이드: `docs/auth-integration-guide.md`

---

## 작성자

- sunm2n
