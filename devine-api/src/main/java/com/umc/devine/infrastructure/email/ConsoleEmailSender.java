package com.umc.devine.infrastructure.email;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("""
                --- [ConsoleEmailSender] Sending Email ---
                To: {}
                Subject: {}
                Body: {}
                -------------------------------------------""", to, subject, body);
    }
}
