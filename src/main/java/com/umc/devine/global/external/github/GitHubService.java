package com.umc.devine.global.external.github;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorCode;
import com.umc.devine.global.external.clerk.ClerkApiClient;
import com.umc.devine.global.external.github.dto.GitHubContributionDTO;
import com.umc.devine.global.external.github.dto.GitHubRepositoryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * GitHub API 통합 서비스
 * Clerk에서 토큰을 조회하고 GitHub API를 호출하는 로직을 캡슐화
 * 다른 도메인에서는 clerkId만으로 GitHub 데이터를 조회할 수 있음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubService {

    private final ClerkApiClient clerkApiClient;
    private final GitHubApiClient githubApiClient;

    @Value("${github.service-token:}")
    private String serviceToken;

    /**
     * GitHub 레포지토리 목록 조회
     *
     * @param clerkId Clerk 사용자 ID
     * @return 레포지토리 목록 (이름, 설명, URL)
     * @throws AuthException GitHub 연동이 없거나 API 호출 실패 시
     */
    public List<GitHubRepositoryDTO> getRepositories(String clerkId) {
        String accessToken = clerkApiClient.getGitHubAccessToken(clerkId);
        return githubApiClient.getRepositories(accessToken);
    }

    /**
     * GitHub Contributions (잔디) 데이터 조회
     * GitHub username을 자동으로 조회하여 사용
     *
     * @param clerkId Clerk 사용자 ID
     * @return 날짜별 기여 목록
     * @throws AuthException GitHub 연동이 없거나 API 호출 실패 시
     */
    public List<GitHubContributionDTO> getContributions(String clerkId) {
        String accessToken = clerkApiClient.getGitHubAccessToken(clerkId);

        // GitHub username 조회
        Map<String, Object> userInfo = githubApiClient.getUserInfo(accessToken);
        String username = (String) userInfo.get("login");

        if (username == null || username.isEmpty()) {
            throw new AuthException(AuthErrorCode.GITHUB_API_ERROR);
        }

        return githubApiClient.getContributions(accessToken, username);
    }

    /**
     * GitHub 사용자 정보 조회
     *
     * @param clerkId Clerk 사용자 ID
     * @return 사용자 정보 (login, name, avatar_url 등)
     * @throws AuthException GitHub 연동이 없거나 API 호출 실패 시
     */
    public Map<String, Object> getUserInfo(String clerkId) {
        String accessToken = clerkApiClient.getGitHubAccessToken(clerkId);
        return githubApiClient.getUserInfo(accessToken);
    }

    // ========== 서비스 토큰 사용 메서드 (다른 사용자 조회용) ==========

    /**
     * 다른 사용자의 GitHub Contributions (잔디) 데이터 조회
     * 서비스 토큰을 사용하여 특정 GitHub 사용자의 잔디를 조회
     * 비로그인 또는 GitHub 연동이 없는 사용자도 호출 가능
     *
     * @param githubUsername 조회할 GitHub 사용자명
     * @return 날짜별 기여 목록, 서비스 토큰이 없으면 빈 목록 반환
     */
    public List<GitHubContributionDTO> getContributionsByUsername(String githubUsername) {
        if (githubUsername == null || githubUsername.isEmpty()) {
            return Collections.emptyList();
        }

        if (serviceToken == null || serviceToken.isEmpty()) {
            log.warn("GitHub service token is not configured");
            return Collections.emptyList();
        }

        try {
            return githubApiClient.getContributions(serviceToken, githubUsername);
        } catch (AuthException e) {
            log.warn("Failed to fetch contributions for {}: {}", githubUsername, e.getMessage());
            return Collections.emptyList();
        }
    }
}
