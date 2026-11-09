package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.github.GitHubApiClient;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GithubGrantRevokeHandlerTest {

    private final ClerkApiClient clerk = mock(ClerkApiClient.class);
    private final GitHubApiClient github = mock(GitHubApiClient.class);
    private final OutboxTaskWriter writer = mock(OutboxTaskWriter.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberExternalCleanupPayload payload = new MemberExternalCleanupPayload("clerk_x", List.of("k"));
    private final OutboxTask task = OutboxTask.pending(OutboxTaskType.GITHUB_GRANT_REVOKE, "{}", LocalDateTime.now());

    @Test
    @DisplayName("OAuth 앱 설정이 없으면 회수를 건너뛰고 Clerk 삭제로 넘어간다")
    void notConfigured_skipsRevoke() {
        given(writer.readPayload(task, MemberExternalCleanupPayload.class)).willReturn(payload);
        GithubGrantRevokeHandler handler = new GithubGrantRevokeHandler(clerk, github, writer, memberRepository, "", "");

        handler.handle(task);

        verifyNoInteractions(clerk, github);
        verify(writer).enqueue(OutboxTaskType.CLERK_USER_DELETE, payload);
    }

    @Test
    @DisplayName("GitHub 연동 토큰이 없으면 회수할 권한이 없으므로 성공으로 보고 넘어간다")
    void tokenNotFound_continues() {
        given(writer.readPayload(task, MemberExternalCleanupPayload.class)).willReturn(payload);
        given(clerk.findGitHubAccessToken("clerk_x")).willReturn(Optional.empty());
        GithubGrantRevokeHandler handler = new GithubGrantRevokeHandler(clerk, github, writer, memberRepository, "id", "secret");

        handler.handle(task);

        verify(github, never()).revokeGrant(anyString(), anyString(), anyString());
        verify(writer).enqueue(OutboxTaskType.CLERK_USER_DELETE, payload);
    }

    @Test
    @DisplayName("Clerk 토큰 조회가 429나 5xx 같은 장애면 예외를 던져 재시도하게 하고 다음 단계는 기록하지 않는다")
    void clerkError_retries() {
        given(writer.readPayload(task, MemberExternalCleanupPayload.class)).willReturn(payload);
        given(clerk.findGitHubAccessToken("clerk_x")).willThrow(new IllegalStateException("Clerk GitHub 토큰 조회 실패: HTTP 429"));
        GithubGrantRevokeHandler handler = new GithubGrantRevokeHandler(clerk, github, writer, memberRepository, "id", "secret");

        assertThatThrownBy(() -> handler.handle(task)).isInstanceOf(IllegalStateException.class);
        verify(writer, never()).enqueue(any(), any());
    }

    @Test
    @DisplayName("같은 Clerk 계정으로 다시 가입한 회원이 있으면 회수하지 않고 다음 단계로 넘어간다")
    void reRegistered_skipsRevoke() {
        given(writer.readPayload(task, MemberExternalCleanupPayload.class)).willReturn(payload);
        given(memberRepository.existsByClerkIdInAnyStatus("clerk_x")).willReturn(true);
        GithubGrantRevokeHandler handler = new GithubGrantRevokeHandler(clerk, github, writer, memberRepository, "id", "secret");

        handler.handle(task);

        verifyNoInteractions(clerk, github);
        verify(writer).enqueue(OutboxTaskType.CLERK_USER_DELETE, payload);
    }
}
