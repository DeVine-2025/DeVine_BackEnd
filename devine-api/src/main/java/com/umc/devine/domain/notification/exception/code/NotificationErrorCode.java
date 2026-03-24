package com.umc.devine.domain.notification.exception.code;

import com.umc.devine.global.apiPayload.code.BaseErrorCode;
import com.umc.devine.global.exception.DomainErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION404_1", "알림을 찾을 수 없습니다.", NotificationErrorReason.NOTIFICATION_NOT_FOUND),
    FORBIDDEN(HttpStatus.FORBIDDEN, "NOTIFICATION403_1", "해당 알림에 대한 권한이 없습니다.", NotificationErrorReason.FORBIDDEN),
    RECEIVER_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION404_2", "알림 수신자를 찾을 수 없습니다.", NotificationErrorReason.RECEIVER_NOT_FOUND),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
    private final DomainErrorReason reason;
}
