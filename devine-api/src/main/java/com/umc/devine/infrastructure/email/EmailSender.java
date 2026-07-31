package com.umc.devine.infrastructure.email;

public interface EmailSender {

    void send(String to, String subject, String body);
}
