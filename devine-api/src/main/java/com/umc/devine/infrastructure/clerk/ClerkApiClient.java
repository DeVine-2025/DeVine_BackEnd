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

@Slf4j
@Component
@RequiredArgsConstructor
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
     * Clerk API를 통해 사용자 삭제 (회원 탈퇴 시 호출).
     *
     * <p>멱등성을 위해 404 응답은 정상으로 간주합니다(이미 삭제된 사용자).
     * 4xx(404 제외)/5xx 응답이나 통신 오류는 {@link AuthException}으로 매핑됩니다.
     *
     * @param clerkUserId Clerk 사용자 ID (user_xxx 형식)
     * @throws AuthException Clerk API 호출 실패 시
     */
    public void deleteUser(String clerkUserId) {
        String clerkSecretKey = env.getProperty("clerk.secret-key");
        String url = CLERK_API_BASE_URL + "/users/" + clerkUserId;

        try {
            restClient.delete()
                    .uri(url)
                    .header("Authorization", "Bearer " + clerkSecretKey)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        // 이미 삭제된 사용자 - 멱등성 보장을 위해 정상 처리
                        log.info("[Clerk] 이미 삭제된 사용자입니다. clerkUserId={}", clerkUserId);
                    })
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        log.warn("[Clerk] 사용자 삭제 4xx 응답. clerkUserId={}, status={}",
                                clerkUserId, response.getStatusCode());
                        throw new AuthException(AuthErrorReason.CLERK_USER_DELETE_FAILED);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.warn("[Clerk] 사용자 삭제 5xx 응답. clerkUserId={}, status={}",
                                clerkUserId, response.getStatusCode());
                        throw new AuthException(AuthErrorReason.CLERK_USER_DELETE_FAILED);
                    })
                    .toBodilessEntity();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[Clerk] 사용자 삭제 호출 중 예외 발생. clerkUserId={}", clerkUserId, e);
            throw new AuthException(AuthErrorReason.CLERK_USER_DELETE_FAILED);
        }
    }
}
