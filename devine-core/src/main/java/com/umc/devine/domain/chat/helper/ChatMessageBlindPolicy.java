package com.umc.devine.domain.chat.helper;

import com.umc.devine.domain.chat.entity.ChatMessage;

import java.util.Set;

public final class ChatMessageBlindPolicy {

    public static final String REPORTED_BY_VIEWER = "신고한 메시지예요.";
    public static final String HIDDEN_BY_POLICY = "운영 정책에 따라 가려진 메시지예요.";

    private ChatMessageBlindPolicy() {
    }

    public static String resolveContent(ChatMessage message, Set<Long> reportedByViewer) {
        if (message.isHiddenByPolicy()) {
            return HIDDEN_BY_POLICY;
        }
        if (reportedByViewer.contains(message.getId())) {
            return REPORTED_BY_VIEWER;
        }
        return message.getContent();
    }
}
