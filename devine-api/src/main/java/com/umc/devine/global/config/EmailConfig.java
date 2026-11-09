package com.umc.devine.global.config;

import com.umc.devine.infrastructure.email.BrevoEmailSender;
import com.umc.devine.infrastructure.email.ConsoleEmailSender;
import com.umc.devine.infrastructure.email.EmailSender;
import com.umc.devine.infrastructure.email.SmtpEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

// 이메일 발송 provider 선택 우선순위: Brevo API 키 -> SMTP 계정 -> Console(로그만 출력)
@Slf4j
@Configuration
public class EmailConfig {

    @Bean
    public EmailSender emailSender(
            @Qualifier("externalApiRestClient") RestClient restClient,
            @Value("${external-api.connect-timeout:3000}") int connectTimeout,
            @Value("${external-api.read-timeout:10000}") int readTimeout,
            @Value("${brevo.api-key:}") String brevoApiKey,
            @Value("${brevo.from-email:}") String brevoFromEmail,
            @Value("${brevo.from-name:}") String brevoFromName,
            @Value("${smtp.host:}") String smtpHost,
            @Value("${smtp.port:587}") int smtpPort,
            @Value("${smtp.user:}") String smtpUser,
            @Value("${smtp.pass:}") String smtpPass,
            @Value("${smtp.from:}") String smtpFrom,
            @Value("${smtp.from-name:}") String smtpFromName
    ) {
        if (StringUtils.hasText(brevoApiKey)) {
            log.info("[EMAIL] BrevoEmailSender를 사용합니다.");
            return new BrevoEmailSender(restClient, brevoApiKey, brevoFromEmail, brevoFromName);
        }
        if (StringUtils.hasText(smtpUser)) {
            log.info("[EMAIL] SmtpEmailSender를 사용합니다.");
            return new SmtpEmailSender(buildMailSender(smtpHost, smtpPort, smtpUser, smtpPass, connectTimeout, readTimeout), smtpFrom, smtpFromName);
        }
        log.warn("[EMAIL] brevo.api-key, smtp.user 모두 설정되지 않아 ConsoleEmailSender를 사용합니다.");
        return new ConsoleEmailSender();
    }

    private JavaMailSenderImpl buildMailSender(String host, int port, String user, String pass,
                                               int connectTimeout, int readTimeout) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(user);
        mailSender.setPassword(pass);

        var props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", String.valueOf(connectTimeout));
        props.put("mail.smtp.timeout", String.valueOf(readTimeout));
        props.put("mail.smtp.writetimeout", String.valueOf(readTimeout));

        return mailSender;
    }
}
