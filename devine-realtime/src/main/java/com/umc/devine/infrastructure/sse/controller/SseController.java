package com.umc.devine.infrastructure.sse.controller;

import com.umc.devine.domain.auth.exception.AuthException;
import com.umc.devine.domain.auth.exception.code.AuthErrorReason;
import com.umc.devine.global.security.ClerkPrincipal;
import com.umc.devine.infrastructure.sse.core.SseEmitterManager;
import com.umc.devine.infrastructure.sse.listener.SseConnectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 연결 관리 컨트롤러
 * OSIV로 인한 DB 커넥션 누수 방지를 위해 JdbcTemplate으로 직접 조회 (JPA EntityManager 우회)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse/v1")
@Slf4j
public class SseController implements SseControllerDocs {

    private final SseEmitterManager sseEmitterManager;
    private final ApplicationEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
        Long memberId = resolveMemberId();

        SseEmitter emitter = sseEmitterManager.create(memberId);

        // 재연결 시 놓친 메시지 처리를 위한 이벤트 발행
        if (lastEventId != null && !lastEventId.isEmpty()) {
            try {
                Long parsedLastEventId = Long.parseLong(lastEventId);
                eventPublisher.publishEvent(new SseConnectedEvent(this, memberId, parsedLastEventId));
            } catch (NumberFormatException e) {
                log.warn("Invalid Last-Event-ID format: {}", lastEventId);
            }
        }

        return emitter;
    }

    // TODO : 추후 open in view 적용 후, SSE 부분은 ID 를 닉네임으로 변경할지 고민

    private Long resolveMemberId() {
        String clerkId = resolveClerkId();

        // JdbcTemplate은 OSIV의 EntityManager와 무관하게 커넥션을 즉시 반납
        Long memberId = jdbcTemplate.query(
                "SELECT member_id FROM member WHERE clerk_id = ? AND used = 'ACTIVE'",
                rs -> rs.next() ? rs.getLong("member_id") : null,
                clerkId
        );

        if (memberId == null) {
            throw new AuthException(AuthErrorReason.NOT_REGISTERED);
        }

        return memberId;
    }

    private String resolveClerkId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException(AuthErrorReason.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof ClerkPrincipal clerkPrincipal)) {
            throw new AuthException(AuthErrorReason.UNAUTHORIZED);
        }

        return clerkPrincipal.getClerkId();
    }
}
