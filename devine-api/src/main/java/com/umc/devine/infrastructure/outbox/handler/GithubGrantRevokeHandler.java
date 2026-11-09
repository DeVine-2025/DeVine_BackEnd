package com.umc.devine.infrastructure.outbox.handler;

import com.umc.devine.domain.outbox.entity.OutboxTask;
import com.umc.devine.domain.outbox.enums.OutboxTaskType;
import com.umc.devine.domain.member.repository.MemberRepository;
import com.umc.devine.infrastructure.clerk.ClerkApiClient;
import com.umc.devine.infrastructure.github.GitHubApiClient;
import com.umc.devine.infrastructure.outbox.OutboxTaskHandler;
import com.umc.devine.infrastructure.outbox.OutboxTaskWriter;
import com.umc.devine.infrastructure.outbox.payload.MemberExternalCleanupPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Component
public class GithubGrantRevokeHandler implements OutboxTaskHandler {

    private final ClerkApiClient clerkApiClient;
    private final GitHubApiClient gitHubApiClient;
    private final OutboxTaskWriter outboxTaskWriter;
    private final MemberRepository memberRepository;
    private final String clientId;
    private final String clientSecret;

    public GithubGrantRevokeHandler(
            ClerkApiClient clerkApiClient,
            GitHubApiClient gitHubApiClient,
            OutboxTaskWriter outboxTaskWriter,
            MemberRepository memberRepository,
            @Value("${github.oauth.client-id:}") String clientId,
            @Value("${github.oauth.client-secret:}") String clientSecret
    ) {
        this.clerkApiClient = clerkApiClient;
        this.gitHubApiClient = gitHubApiClient;
        this.outboxTaskWriter = outboxTaskWriter;
        this.memberRepository = memberRepository;
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
        } else if (memberRepository.existsByClerkIdInAnyStatus(payload.clerkId())) {
            log.warn("[Outbox] 같은 Clerk 계정으로 다시 가입한 회원이 있어 권한 회수를 건너뜀 - taskId: {}", task.getId());
        } else {
            revoke(payload, task.getId());
        }

        nextStep(payload);
    }

    private void revoke(MemberExternalCleanupPayload payload, Long taskId) {
        Optional<String> token = clerkApiClient.findGitHubAccessToken(payload.clerkId());
        if (token.isEmpty()) {
            log.info("[Outbox] GitHub 연동 토큰이 없어 회수할 권한 없음 - taskId: {}", taskId);
            return;
        }
        gitHubApiClient.revokeGrant(clientId, clientSecret, token.get());
    }

    @Override
    public void onFinalFailure(OutboxTask task) {
        nextStep(outboxTaskWriter.readPayload(task, MemberExternalCleanupPayload.class));
    }

    private void nextStep(MemberExternalCleanupPayload payload) {
        outboxTaskWriter.enqueue(OutboxTaskType.CLERK_USER_DELETE, payload);
    }
}
