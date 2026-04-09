package com.umc.devine.domain.chat.controller;

import com.umc.devine.domain.chat.dto.ChatReqDTO;
import com.umc.devine.domain.chat.service.command.ChatCommandService;
import com.umc.devine.infrastructure.chat.auth.ChatPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatCommandService chatCommandService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Valid ChatReqDTO.SendMessageReq request,
            Principal principal
    ) {
        ChatPrincipal chatPrincipal = (ChatPrincipal) principal;
        chatCommandService.sendMessage(chatPrincipal.getMemberId(), roomId, request.content());
    }

    @MessageMapping("/chat/{roomId}/read")
    public void markAsRead(
            @DestinationVariable Long roomId,
            Principal principal
    ) {
        ChatPrincipal chatPrincipal = (ChatPrincipal) principal;
        chatCommandService.markAsRead(chatPrincipal.getMemberId(), roomId);
    }

    @MessageExceptionHandler
    public void handleException(Exception ex, Principal principal) {
        String username = principal != null ? principal.getName() : "unknown";
        log.error("WebSocket 메시지 처리 오류 - user: {}", username, ex);
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    Map.of("error", ex.getMessage())
            );
        }
    }
}
