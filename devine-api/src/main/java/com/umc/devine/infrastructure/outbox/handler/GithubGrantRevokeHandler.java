package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorReason;
import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.github.GitHubApiClient;
import com.umc.devine.infrastructure.outbox.OutboxTaskHandler;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Clerk에서 토큰을 받아야 하므로 Clerk 사용자 삭제보다 먼저 실행한다. */
@Slf4j
@Component
public class GithubGrantRevokeHandler implements OutboxTaskHandler {

    private final ClerkApiClient clerkApiClient;
    private final GitHubApiClient gitHubApiClient;
    private final OutboxTaskWriter outboxTaskWriter;
    private final String clientId;
    private final String clientSecret;

    public GithubGrantRevokeHandler(
            ClerkApiClient clerkApiClient,
            GitHubApiClient gitHubApiClient,
            OutboxTaskWriter outboxTaskWriter,
            @Value("${github.oauth.client-id:}") String clientId,
            @Value("${github.oauth.client-secret:}") String clientSecret
    ) {
        this.clerkApiClient = clerkApiClient;
        this.gitHubApiClient = gitHubApiClient;
        this.outboxTaskWriter = outboxTaskWriter;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public OutboxTaskType type() {
        return OutboxTaskType.GITHUB_GRANT_REVOKE;
    }

    @Override
    public void handle(OutboxTask task) {
        MemberExternalCleanupPayload payload = outboxTaskWriter.readPayload(task, MemberExternalCleanupPayload.class);

        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            log.info("[Outbox] GitHub OAuth 앱 설정이 없어 권한 회수를 건너뜀 - taskId: {}", task.getId());
        } else {
            revoke(payload, task.getId());
        }

        nextStep(payload);
    }

    private void revoke(MemberExternalCleanupPayload payload, Long taskId) {
        String token;
        try {
            token = clerkApiClient.getGitHubAccessToken(payload.clerkId());
        } catch (AuthException e) {
            if (e.getReason() == AuthErrorReason.GITHUB_TOKEN_NOT_FOUND) {
                log.info("[Outbox] GitHub 연동 토큰이 없어 회수할 권한 없음 - taskId: {}", taskId);
                return;
            }
            throw e;
        }
        gitHubApiClient.revokeGrant(clientId, clientSecret, token);
    }

    // 회수가 최종 실패해도 개인정보가 남은 Clerk 사용자는 지워야 하므로 다음 단계로 넘긴다
    @Override
    public void onFinalFailure(OutboxTask task) {
        nextStep(outboxTaskWriter.readPayload(task, MemberExternalCleanupPayload.class));
    }

    private void nextStep(MemberExternalCleanupPayload payload) {
        outboxTaskWriter.enqueue(OutboxTaskType.CLERK_USER_DELETE, payload);
    }
}
