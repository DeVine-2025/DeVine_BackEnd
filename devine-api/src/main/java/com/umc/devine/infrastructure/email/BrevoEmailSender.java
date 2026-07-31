package com.umc.devine.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
public class BrevoEmailSender implements EmailSender {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoEmailSender(RestClient restClient, String apiKey, String fromEmail, String fromName) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
    }

    @Override
    public void send(String to, String subject, String body) {
        Map<String, Object> requestBody = Map.of(
                "sender", Map.of("name", fromName, "email", fromEmail),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "htmlContent", body
        );

        try {
            restClient.post()
                    .uri(BREVO_API_URL)
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IllegalStateException("Brevo API error: " + response.getStatusCode());
                    })
                    .toBodilessEntity();
            log.info("[BrevoEmailSender] 이메일 발송 성공 - to: {}", to);
        } catch (Exception e) {
            log.error("[BrevoEmailSender] 이메일 발송 실패 - to: {}, error: {}", to, e.getMessage());
        }
    }
}
