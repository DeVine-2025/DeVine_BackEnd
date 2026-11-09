package com.umc.devine.infrastructure.email;

public interface EmailSender {

    /** 발송 실패는 로그만 남기고 삼킨다. */
    void send(String to, String subject, String body);

    /** 실패를 예외로 알린다. 재시도가 필요한 곳(아웃박스)에서 쓴다. */
    void sendOrThrow(String to, String subject, String body);
}
