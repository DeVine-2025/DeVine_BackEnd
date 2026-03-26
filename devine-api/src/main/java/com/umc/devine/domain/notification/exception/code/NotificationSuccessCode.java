package com.umc.devine.domain.notification.exception.code;

import com.umc.devine.global.apiPayload.code.BaseSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationSuccessCode implements BaseSuccessCode {

    FETCH_SUCCESS(HttpStatus.OK,
            "NOTIFICATION200_1",
            "알림 조회에 성공했습니다."),
    MARK_READ_SUCCESS(HttpStatus.OK,
            "NOTIFICATION200_2",
            "알림 읽음 처리에 성공했습니다."),
    MARK_ALL_READ_SUCCESS(HttpStatus.OK,
            "NOTIFICATION200_3",
            "전체 알림 읽음 처리에 성공했습니다."),
    SSE_CONNECT_SUCCESS(HttpStatus.OK,
            "NOTIFICATION200_4",
            "SSE 연결에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
