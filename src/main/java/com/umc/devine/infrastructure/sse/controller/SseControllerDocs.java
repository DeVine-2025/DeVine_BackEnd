package com.umc.devine.infrastructure.sse.controller;

import com.umc.devine.domain.member.entity.Member;
import com.umc.devine.global.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE", description = "Server-Sent Events 실시간 연결 API")
public interface SseControllerDocs {

    @Operation(
            summary = "SSE 연결",
            description = """
                    실시간 이벤트 수신을 위한 SSE 연결을 생성합니다.

                    **이벤트 타입**:
                    - `connect`: 연결 성공
                    - `notification`: 새 알림
                    - `heartbeat`: 연결 유지 (30초 간격)
                    - `shutdown`: 서버 종료

                    **재연결**: Last-Event-ID 헤더로 놓친 알림을 자동 수신합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "SSE 스트림 연결 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증이 필요합니다."
            )
    })
    SseEmitter subscribe(
            @Parameter(hidden = true) @CurrentMember Member member,
            @Parameter(description = "마지막 수신 이벤트 ID (재연결 시 자동 전송)")
            String lastEventId
    );
}
