package com.umc.devine.infrastructure.email;

public interface EmailSender {

    /** 발송 실패는 로그만 남기고 삼킨다. */
    void send(String to, String subject, String body);

    /**
     * 발송 실패를 예외로 알린다. 실패하면 다시 보내야 하는 곳(아웃박스 재시도)에서 쓴다.
     *
     * @throws EmailSendException 발송에 실패한 경우
     */
    void sendOrThrow(String to, String subject, String body);
}
