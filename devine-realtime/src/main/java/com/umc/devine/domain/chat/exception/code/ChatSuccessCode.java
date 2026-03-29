package com.umc.devine.domain.chat.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatSuccessCode implements BaseSuccessCode {
    ROOM_CREATED(HttpStatus.CREATED, "CHAT201_1", "채팅방이 생성되었습니다."),
    ROOM_LIST_FETCHED(HttpStatus.OK, "CHAT200_1", "채팅방 목록 조회에 성공했습니다."),
    MESSAGES_FETCHED(HttpStatus.OK, "CHAT200_2", "메시지 조회에 성공했습니다."),
    READ_SUCCESS(HttpStatus.OK, "CHAT200_3", "읽음 처리에 성공했습니다."),
    LEAVE_SUCCESS(HttpStatus.OK, "CHAT200_4", "채팅방 나가기에 성공했습니다."),
    UNREAD_COUNT_FETCHED(HttpStatus.OK, "CHAT200_5", "안읽은 채팅방 수 조회에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
