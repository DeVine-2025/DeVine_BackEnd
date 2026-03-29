package com.umc.devine.infrastructure.chat.auth;

import com.umc.devine.domain.chat.exception.ChatException;
import com.umc.devine.domain.chat.exception.code.ChatErrorReason;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT 인증 실패: Authorization 헤더 없음");
                throw new ChatException(ChatErrorReason.WEBSOCKET_MISSING_AUTH_HEADER);
            }

            String token = authHeader.substring(7);

            try {
                Jwt jwt = jwtDecoder.decode(token);
                String clerkId = jwt.getSubject();

                Long memberId = jdbcTemplate.query(
                        "SELECT member_id FROM member WHERE clerk_id = ? AND used = 'ACTIVE'",
                        rs -> rs.next() ? rs.getLong("member_id") : null,
                        clerkId
                );

                if (memberId == null) {
                    throw new ChatException(ChatErrorReason.WEBSOCKET_MEMBER_NOT_FOUND);
                }

                ChatPrincipal principal = new ChatPrincipal(clerkId, memberId);
                accessor.setUser(principal);

                log.debug("WebSocket CONNECT 인증 성공 - clerkId: {}, memberId: {}", clerkId, memberId);

            } catch (ChatException e) {
                throw e;
            } catch (Exception e) {
                log.error("WebSocket CONNECT 인증 실패", e);
                throw new ChatException(ChatErrorReason.WEBSOCKET_AUTH_FAILED);
            }
        }

        return message;
    }
}
