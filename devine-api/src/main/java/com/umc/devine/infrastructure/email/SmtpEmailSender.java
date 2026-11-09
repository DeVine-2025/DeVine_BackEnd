package com.umc.devine.infrastructure.email;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Slf4j
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String fromName;

    public SmtpEmailSender(JavaMailSender mailSender, String fromEmail, String fromName) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            sendOrThrow(to, subject, body);
        } catch (EmailSendException e) {
            // 기존 동작 유지: 실패는 로그만 남긴다(sendOrThrow에서 이미 남김)
        }
    }

    @Override
    public void sendOrThrow(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("[SmtpEmailSender] 이메일 발송 성공 - to: {}", to);
        } catch (Exception e) {
            log.error("[SmtpEmailSender] 이메일 발송 실패 - to: {}, error: {}", to, e.getMessage());
            throw new EmailSendException("SMTP 이메일 발송 실패", e);
        }
    }
}
