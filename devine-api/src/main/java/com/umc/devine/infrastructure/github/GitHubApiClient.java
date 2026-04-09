package com.umc.devine.infrastructure.github;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorReason;
import com.umc.devine.infrastructure.github.dto.GitHubContributionDTO;
import com.umc.devine.infrastructure.github.dto.GitHubRepositoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GitHubApiClient {

    private static final String GITHUB_API_BASE_URL = "https://api.github.com";

    private final RestClient restClient;

    /**
     * GitHub 사용자 정보 조회
     *
     * @param accessToken GitHub OAuth Access Token
     * @return 사용자 정보 Map
     */
    public Map<String, Object> getUserInfo(String accessToken) {
        String url = GITHUB_API_BASE_URL + "/user";

        try {
            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                    })
                    .body(new ParameterizedTypeReference<>() {});
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
        }
    }

    /**
     * GitHub 사용자의 모든 이메일 조회
     *
     * @param accessToken GitHub OAuth Access Token
     * @return 이메일 목록 (primary 이메일이 첫 번째)
     */
    @SuppressWarnings("unchecked")
    public List<String> getUserEmails(String accessToken) {
        String url = GITHUB_API_BASE_URL + "/user/emails";

        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                    })
                    .body(new ParameterizedTypeReference<>() {});

            if (emails == null || emails.isEmpty()) {
                return List.of();
            }

            // primary 이메일을 첫 번째로 배치
            List<String> result = new ArrayList<>();
            String primaryEmail = null;

            for (Map<String, Object> emailInfo : emails) {
                String email = (String) emailInfo.get("email");
                if (email == null) continue;
                if (Boolean.TRUE.equals(emailInfo.get("primary"))) {
                    primaryEmail = email;
                } else {
                    result.add(email);
                }
            }

            if (primaryEmail != null) {
                result.add(0, primaryEmail);
            }

            return result;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
        }
    }

    /**
     * GitHub 사용자의 모든 이메일 조회 (noreply 이메일 포함)
     *
     * @param accessToken GitHub OAuth Access Token
     * @return 중복 제거된 이메일 리스트 (primary 이메일이 첫 번째, noreply 포함)
     */
    public List<String> getAllAuthorEmails(String accessToken) {
        Map<String, Object> userInfo = getUserInfo(accessToken);
        String login = (String) userInfo.get("login");
        Object idObj = userInfo.get("id");
        String noreplyEmail = idObj + "+" + login + "@users.noreply.github.com";

        List<String> emails = getUserEmails(accessToken);

        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String email : emails) {
            if (seen.add(email.toLowerCase())) {
                result.add(email);
            }
        }
        if (seen.add(noreplyEmail.toLowerCase())) {
            result.add(noreplyEmail);
        }

        return result;
    }

    /**
     * GitHub 레포지토리 목록 조회 (페이지네이션 처리)
     *
     * @param accessToken GitHub OAuth Access Token
     * @return 레포지토리 목록
     * @throws AuthException GitHub API 호출 실패 시
     */
    public List<GitHubRepositoryDTO> getRepositories(String accessToken) {
        List<GitHubRepositoryDTO> allRepositories = new ArrayList<>();
        int page = 1;
        int perPage = 100;

        try {
            while (true) {
                String url = GITHUB_API_BASE_URL + "/user/repos?per_page=" + perPage + "&page=" + page + "&sort=created&direction=desc";

                List<GitHubRepositoryDTO> repositories = restClient.get()
                        .uri(url)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                            throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                            throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                        })
                        .body(new ParameterizedTypeReference<>() {});

                if (repositories == null || repositories.isEmpty()) {
                    break;
                }

                allRepositories.addAll(repositories);

                if (repositories.size() < perPage) {
                    break;
                }

                page++;
            }

            return allRepositories;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
        }
    }

    /**
     * GitHub Contributions (잔디) 데이터 조회
     * GraphQL API를 사용하여 사용자의 기여 활동 데이터를 조회
     *
     * @param accessToken GitHub OAuth Access Token
     * @param username GitHub 사용자명
     * @param from 시작 날짜, null이면 기본값 사용
     * @param to 종료 날짜, null이면 기본값 사용
     * @return 날짜별 기여 목록
     * @throws AuthException GitHub API 호출 실패 시
     */
    public List<GitHubContributionDTO> getContributions(String accessToken, String username, LocalDate from, LocalDate to) {
        if (username == null || username.isBlank()) {
            throw new AuthException(AuthErrorReason.GITHUB_USER_NOT_FOUND);
        }

        String url = GITHUB_API_BASE_URL + "/graphql";

        String dateParams = "";
        if (from != null && to != null) {
            dateParams = "(from: \"%s\", to: \"%s\")".formatted(
                    from.atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT),
                    to.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
            );
        } else if (from != null) {
            dateParams = "(from: \"%s\")".formatted(
                    from.atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
            );
        } else if (to != null) {
            dateParams = "(to: \"%s\")".formatted(
                    to.atTime(23, 59, 59).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
            );
        }

        String query = """
                query {
                  user(login: "%s") {
                    contributionsCollection%s {
                      contributionCalendar {
                        weeks {
                          contributionDays {
                            date
                            contributionCount
                          }
                        }
                      }
                    }
                  }
                }
                """.formatted(username, dateParams);

        Map<String, Object> requestBody = Map.of("query", query);

        try {
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response1) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response1) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                    })
                    .body(new ParameterizedTypeReference<>() {});

            return parseContributionsFromGraphQL(response);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
        }
    }

    /**
     * GraphQL 응답에서 Contribution 데이터 파싱
     *
     * @param response GraphQL API 응답
     * @return 날짜별 기여 목록
     * @throws AuthException 존재하지 않는 사용자(GITHUB_USER_NOT_FOUND) 또는 응답 파싱 실패(GITHUB_API_ERROR)
     */
    @SuppressWarnings("unchecked")
    private List<GitHubContributionDTO> parseContributionsFromGraphQL(Map<String, Object> response) {
        Map<String, Object> user = Optional.ofNullable(response)
                .map(r -> (Map<String, Object>) r.get("data"))
                .map(data -> (Map<String, Object>) data.get("user"))
                .orElseThrow(() -> new AuthException(AuthErrorReason.GITHUB_USER_NOT_FOUND));

        List<Map<String, Object>> weeks = Optional.of(user)
                .map(u -> (Map<String, Object>) u.get("contributionsCollection"))
                .map(collection -> (Map<String, Object>) collection.get("contributionCalendar"))
                .map(calendar -> (List<Map<String, Object>>) calendar.get("weeks"))
                .orElseThrow(() -> new AuthException(AuthErrorReason.GITHUB_API_ERROR));

        return parseWeeks(weeks);
    }

    /**
     * weeks 데이터에서 ContributionDTO 리스트 추출
     */
    @SuppressWarnings("unchecked")
    private List<GitHubContributionDTO> parseWeeks(List<Map<String, Object>> weeks) {
        List<GitHubContributionDTO> contributions = new ArrayList<>();

        for (Map<String, Object> week : weeks) {
            if (week == null) {
                continue;
            }

            List<Map<String, Object>> contributionDays = (List<Map<String, Object>>) week.get("contributionDays");
            if (contributionDays == null) {
                continue;
            }

            for (Map<String, Object> day : contributionDays) {
                if (day == null) {
                    continue;
                }

                String dateStr = (String) day.get("date");
                LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : null;
                Integer count = (Integer) day.get("contributionCount");

                contributions.add(GitHubContributionDTO.builder()
                        .date(date)
                        .contributionCount(count)
                        .build());
            }
        }

        return contributions;
    }

    /**
     * 내가 소유한 레포 + 기여한 레포 목록 조회 (GraphQL API 사용)
     * - 내가 소유한 레포지토리 (ownerAffiliations: OWNER)
     * - 내가 커밋/PR로 기여한 레포지토리 (repositoriesContributedTo)
     *
     * @param accessToken GitHub OAuth Access Token
     * @return 레포지토리 목록 (중복 제거됨)
     * @throws AuthException GitHub API 호출 실패 시
     */
    public List<GitHubRepositoryDTO> getContributedRepositories(String accessToken) {
        String url = GITHUB_API_BASE_URL + "/graphql";

        Set<String> seenUrls = new HashSet<>();
        List<GitHubRepositoryDTO> allRepositories = new ArrayList<>();

        // 1. 내가 소유한 레포 조회 (페이지네이션)
        String ownedCursor = null;
        do {
            String afterClause = ownedCursor != null ? ", after: \"%s\"".formatted(ownedCursor) : "";
            String query = """
                    query {
                      viewer {
                        repositories(first: 100, ownerAffiliations: [OWNER], orderBy: {field: CREATED_AT, direction: DESC}%s) {
                          pageInfo {
                            hasNextPage
                            endCursor
                          }
                          nodes {
                            name
                            url
                            description
                          }
                        }
                      }
                    }
                    """.formatted(afterClause);

            Map<String, Object> response = executeGraphQL(accessToken, url, query);
            ownedCursor = parseRepositoriesPage(response, "repositories", allRepositories, seenUrls);
        } while (ownedCursor != null);

        // 2. 내가 기여한 레포 조회 (페이지네이션)
        String contributedCursor = null;
        do {
            String afterClause = contributedCursor != null ? ", after: \"%s\"".formatted(contributedCursor) : "";
            String query = """
                    query {
                      viewer {
                        repositoriesContributedTo(first: 100, contributionTypes: [COMMIT, PULL_REQUEST], orderBy: {field: CREATED_AT, direction: DESC}%s) {
                          pageInfo {
                            hasNextPage
                            endCursor
                          }
                          nodes {
                            name
                            url
                            description
                          }
                        }
                      }
                    }
                    """.formatted(afterClause);

            Map<String, Object> response = executeGraphQL(accessToken, url, query);
            contributedCursor = parseRepositoriesPage(response, "repositoriesContributedTo", allRepositories, seenUrls);
        } while (contributedCursor != null);

        return allRepositories;
    }

    /**
     * GraphQL 쿼리 실행
     */
    private Map<String, Object> executeGraphQL(String accessToken, String url, String query) {
        Map<String, Object> requestBody = Map.of("query", query);

        try {
            return restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
                    })
                    .body(new ParameterizedTypeReference<>() {});
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorReason.GITHUB_API_ERROR);
        }
    }

    /**
     * GraphQL 응답에서 레포지토리 페이지 파싱
     *
     * @param response GraphQL 응답
     * @param fieldName 조회할 필드명 (repositories 또는 repositoriesContributedTo)
     * @param repositories 결과를 추가할 리스트
     * @param seenUrls 중복 확인용 Set
     * @return 다음 페이지 커서 (없으면 null)
     */
    @SuppressWarnings("unchecked")
    private String parseRepositoriesPage(Map<String, Object> response, String fieldName,
                                         List<GitHubRepositoryDTO> repositories, Set<String> seenUrls) {
        Map<String, Object> viewer = Optional.ofNullable(response)
                .map(r -> (Map<String, Object>) r.get("data"))
                .map(data -> (Map<String, Object>) data.get("viewer"))
                .orElseThrow(() -> new AuthException(AuthErrorReason.GITHUB_API_ERROR));

        Map<String, Object> reposData = (Map<String, Object>) viewer.get(fieldName);
        if (reposData == null) {
            return null;
        }

        List<Map<String, Object>> nodes = (List<Map<String, Object>>) reposData.get("nodes");
        if (nodes != null) {
            for (Map<String, Object> node : nodes) {
                if (node == null) continue;

                String url = (String) node.get("url");
                if (url != null && !seenUrls.contains(url)) {
                    seenUrls.add(url);
                    repositories.add(GitHubRepositoryDTO.builder()
                            .name((String) node.get("name"))
                            .htmlUrl(url)
                            .description((String) node.get("description"))
                            .build());
                }
            }
        }

        Map<String, Object> pageInfo = (Map<String, Object>) reposData.get("pageInfo");
        if (pageInfo != null && Boolean.TRUE.equals(pageInfo.get("hasNextPage"))) {
            return (String) pageInfo.get("endCursor");
        }

        return null;
    }
}
