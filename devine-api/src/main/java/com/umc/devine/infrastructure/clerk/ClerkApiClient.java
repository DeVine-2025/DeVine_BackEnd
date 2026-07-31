package com.umc.devine.infrastructure.clerk;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorReason;
import com.umc.devine.infrastructure.clerk.dto.ClerkOAuthTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClerkApiClient {

    private static final String CLERK_API_BASE_URL = "https://api.clerk.com/v1";

    private final RestClient restClient;
    private final Environment env;

    /**
     * Clerk API를 통해 사용자의 GitHub OAuth Access Token 조회
     *
     * @param clerkUserId Clerk 사용자 ID (user_xxx 형식)
     * @return GitHub Access Token
     * @throws AuthException GitHub 연동 정보가 없거나 API 호출 실패 시
     */
    public String getGitHubAccessToken(String clerkUserId) {
        String clerkSecretKey = env.getProperty("clerk.secret-key");
        String url = CLERK_API_BASE_URL + "/users/" + clerkUserId + "/oauth_access_tokens/oauth_github";

        try {
            List<ClerkOAuthTokenResponse> tokens = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + clerkSecretKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new AuthException(AuthErrorReason.GITHUB_TOKEN_NOT_FOUND);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new AuthException(AuthErrorReason.CLERK_API_ERROR);
                    })
                    .body(new ParameterizedTypeReference<>() {});

            if (tokens == null || tokens.isEmpty()) {
                throw new AuthException(AuthErrorReason.GITHUB_TOKEN_NOT_FOUND);
            }

            String accessToken = tokens.get(0).getToken();

            if (accessToken == null || accessToken.isEmpty()) {
                throw new AuthException(AuthErrorReason.GITHUB_TOKEN_NOT_FOUND);
            }

            return accessToken;

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException(AuthErrorReason.GITHUB_TOKEN_FETCH_FAILED);
        }
    }

    /**
     * Clerk 사용자를 삭제한다. 이미 삭제되어 404가 반환되는 경우도 멱등하게 성공 처리한다
     * (네트워크 재시도나 관리자의 수동 삭제와 중복 호출되어도 안전하도록).
     *
     * @param clerkUserId Clerk 사용자 ID (user_xxx 형식)
     */
    public void deleteUser(String clerkUserId) {
        String clerkSecretKey = env.getProperty("clerk.secret-key");
        String url = CLERK_API_BASE_URL + "/users/" + clerkUserId;

        try {
            restClient.delete()
                    .uri(url)
                    .header("Authorization", "Bearer " + clerkSecretKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.info("[Clerk] 사용자가 이미 삭제됨 - clerkId={}", clerkUserId);
        } catch (Exception e) {
            throw new AuthException(AuthErrorReason.CLERK_API_ERROR);
        }
    }
}
