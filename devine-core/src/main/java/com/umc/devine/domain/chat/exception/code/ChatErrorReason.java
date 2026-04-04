package com.umc.devine.domain.chat.exception.code;

import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorReason implements DomainErrorReason {
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404_1", "채팅방을 찾을 수 없습니다."),
    NOT_CHAT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "CHAT403_1", "해당 채팅방의 멤버가 아닙니다."),
    CANNOT_CHAT_SELF(HttpStatus.BAD_REQUEST, "CHAT400_1", "자기 자신에게 채팅을 보낼 수 없습니다."),
    BOTH_LEFT_ROOM(HttpStatus.BAD_REQUEST, "CHAT400_2", "양쪽 모두 나간 채팅방입니다."),
    TARGET_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT404_2", "대상 회원을 찾을 수 없습니다."),
    WEBSOCKET_MISSING_AUTH_HEADER(HttpStatus.UNAUTHORIZED, "CHAT401_1", "Authorization 헤더가 필요합니다."),
    WEBSOCKET_MEMBER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "CHAT401_2", "등록되지 않은 사용자입니다."),
    WEBSOCKET_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "CHAT401_3", "인증에 실패했습니다."),
    INVALID_MESSAGE_CONTENT(HttpStatus.BAD_REQUEST, "CHAT400_3", "메시지 내용이 유효하지 않습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
