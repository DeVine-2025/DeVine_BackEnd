package com.umc.devine.domain.chat.helper;

import com.umc.devine.domain.chat.entity.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FUNC-REP-003 채팅 메시지 가림 기준")
class ChatMessageBlindPolicyTest {

    @Test
    @DisplayName("내가 신고한 메시지는 내 화면에서만 가려진다")
    void reportedByViewer() {
        ChatMessage message = message(1L);

        assertThat(ChatMessageBlindPolicy.resolveContent(message, Set.of(1L)))
                .isEqualTo(ChatMessageBlindPolicy.REPORTED_BY_VIEWER);
        assertThat(ChatMessageBlindPolicy.resolveContent(message, Set.of()))
                .isEqualTo("원문");
    }

    @Test
    @DisplayName("삭제·정지로 처리된 메시지는 신고 여부와 무관하게 양쪽에서 가려진다")
    void hiddenByPolicy() {
        ChatMessage message = message(2L);
        message.hideByPolicy();

        assertThat(ChatMessageBlindPolicy.resolveContent(message, Set.of()))
                .isEqualTo(ChatMessageBlindPolicy.HIDDEN_BY_POLICY);
        assertThat(ChatMessageBlindPolicy.resolveContent(message, Set.of(2L)))
                .isEqualTo(ChatMessageBlindPolicy.HIDDEN_BY_POLICY);
    }

    private ChatMessage message(Long id) {
        return ChatMessage.builder()
                .id(id)
                .content("원문")
                .build();
    }
}
