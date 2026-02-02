package com.umc.devine.global.external.github;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorCode;
import com.umc.devine.global.external.github.dto.GitHubContributionDTO;
import com.umc.devine.global.external.github.dto.GitHubRepositoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                        throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
                    })
                    .body(new ParameterizedTypeReference<>() {});
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
        }
    }

    /**
     * GitHub 레포지토리 목록 조회
     *
     * @param accessToken GitHub OAuth Access Token
     * @return 레포지토리 목록
     * @throws AuthException GitHub API 호출 실패 시
     */
    public List<GitHubRepositoryDTO> getRepositories(String accessToken) {
        String url = GITHUB_API_BASE_URL + "/user/repos";

        try {
            List<GitHubRepositoryDTO> repositories = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
                    })
                    .body(new ParameterizedTypeReference<>() {});

            return repositories != null ? repositories : new ArrayList<>();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
        }
    }

    /**
     * GitHub Contributions (잔디) 데이터 조회
     * GraphQL API를 사용하여 사용자의 기여 활동 데이터를 조회
     *
     * @param accessToken GitHub OAuth Access Token
     * @param username GitHub 사용자명
     * @return 날짜별 기여 목록
     * @throws AuthException GitHub API 호출 실패 시
     */
    public List<GitHubContributionDTO> getContributions(String accessToken, String username) {
        if (username == null || username.isBlank()) {
            throw new AuthException(AuthErrorCode.GITHUB_USER_NOT_FOUND);
        }

        String url = GITHUB_API_BASE_URL + "/graphql";

        String query = """
                query {
                  user(login: "%s") {
                    contributionsCollection {
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
                """.formatted(username);

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
                        throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response1) -> {
                        throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
                    })
                    .body(new ParameterizedTypeReference<>() {});

            return parseContributionsFromGraphQL(response);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
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
                .orElseThrow(() -> new AuthException(AuthErrorCode.GITHUB_USER_NOT_FOUND));

        List<Map<String, Object>> weeks = Optional.of(user)
                .map(u -> (Map<String, Object>) u.get("contributionsCollection"))
                .map(collection -> (Map<String, Object>) collection.get("contributionCalendar"))
                .map(calendar -> (List<Map<String, Object>>) calendar.get("weeks"))
                .orElseThrow(() -> new AuthException(AuthErrorCode.GITHUB_API_ERROR));

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

                String date = (String) day.get("date");
                Integer count = (Integer) day.get("contributionCount");

                contributions.add(GitHubContributionDTO.builder()
                        .date(date)
                        .contributionCount(count)
                        .build());
            }
        }

        return contributions;
    }
}
