package com.umc.devine.infrastructure.sse.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.security.CurrentMember;
import com.umc.devine.infrastructure.sse.core.SseEmitterManager;
import com.umc.devine.infrastructure.sse.listener.SseConnectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 연결 관리 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse/v1")
@Slf4j
public class SseController implements SseControllerDocs {

    private final SseEmitterManager sseEmitterManager;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @CurrentMember Member member,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
        Long memberId = member.getId();

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
}
