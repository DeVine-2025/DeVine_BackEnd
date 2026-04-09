package com.umc.devine.infrastructure.clerk;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorReason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@DisplayName("ClerkApiClient")
class ClerkApiClientTest {

    private static final String CLERK_USER_ID = "user_test_123";
    private static final String CLERK_SECRET_KEY = "sk_test_secret";
    private static final String DELETE_URL = "https://api.clerk.com/v1/users/" + CLERK_USER_ID;

    private MockRestServiceServer mockServer;
    private ClerkApiClient clerkApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        Environment env = mock(Environment.class);
        when(env.getProperty("clerk.secret-key")).thenReturn(CLERK_SECRET_KEY);

        clerkApiClient = new ClerkApiClient(restClient, env);
    }

    @org.junit.jupiter.api.Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("200 응답이면 정상 종료한다")
        void deleteUser_success() {
            mockServer.expect(requestTo(DELETE_URL))
                    .andExpect(method(org.springframework.http.HttpMethod.DELETE))
                    .andExpect(header("Authorization", "Bearer " + CLERK_SECRET_KEY))
                    .andRespond(withStatus(HttpStatus.OK)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"deleted\":true}"));

            assertThatCode(() -> clerkApiClient.deleteUser(CLERK_USER_ID))
                    .doesNotThrowAnyException();

            mockServer.verify();
        }

        @Test
        @DisplayName("404 응답이면 멱등성 보장을 위해 정상 종료한다")
        void deleteUser_notFound_isIdempotent() {
            mockServer.expect(requestTo(DELETE_URL))
                    .andExpect(method(org.springframework.http.HttpMethod.DELETE))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThatCode(() -> clerkApiClient.deleteUser(CLERK_USER_ID))
                    .doesNotThrowAnyException();

            mockServer.verify();
        }

        @Test
        @DisplayName("4xx(404 제외) 응답이면 CLERK_USER_DELETE_FAILED 예외를 던진다")
        void deleteUser_4xx_throws() {
            mockServer.expect(requestTo(DELETE_URL))
                    .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

            assertThatThrownBy(() -> clerkApiClient.deleteUser(CLERK_USER_ID))
                    .isInstanceOf(AuthException.class)
                    .extracting("reason")
                    .isEqualTo(AuthErrorReason.CLERK_USER_DELETE_FAILED);

            mockServer.verify();
        }

        @Test
        @DisplayName("5xx 응답이면 CLERK_USER_DELETE_FAILED 예외를 던진다")
        void deleteUser_5xx_throws() {
            mockServer.expect(requestTo(DELETE_URL))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> clerkApiClient.deleteUser(CLERK_USER_ID))
                    .isInstanceOf(AuthException.class)
                    .extracting("reason")
                    .isEqualTo(AuthErrorReason.CLERK_USER_DELETE_FAILED);

            mockServer.verify();
        }
    }
}
