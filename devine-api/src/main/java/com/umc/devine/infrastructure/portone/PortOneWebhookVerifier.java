package com.umc.devine.infrastructure.portone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Component
public class PortOneWebhookVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long TOLERANCE_SECONDS = 300; // 5분

    private final byte[] secretBytes;

    public PortOneWebhookVerifier(
            @Value("${portone.webhook-secret:}") String webhookSecret
    ) {
        // PortOne V2 webhook secret은 "whsec_" 접두사 + Base64 인코딩된 키
        String rawSecret = webhookSecret.startsWith("whsec_")
                ? webhookSecret.substring(6)
                : webhookSecret;
        this.secretBytes = Base64.getDecoder().decode(rawSecret);
    }

    /**
     * PortOne V2 Standard Webhook 서명 검증
     * 서명 대상: "{webhookId}.{webhookTimestamp}.{body}"
     */
    public boolean verify(String webhookId, String webhookTimestamp, String webhookSignature, String body) {
        // 타임스탬프 유효성 (리플레이 공격 방지)
        long timestamp;
        try {
            timestamp = Long.parseLong(webhookTimestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - timestamp) > TOLERANCE_SECONDS) {
            return false;
        }

        // HMAC-SHA256 서명 생성
        String signedContent = webhookId + "." + webhookTimestamp + "." + body;
        String expectedSignature;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            byte[] hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
            expectedSignature = "v1," + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return false;
        }

        // 서명 목록에서 하나라도 일치하면 통과 (PortOne은 여러 서명을 space로 구분)
        byte[] expectedBytes = expectedSignature.getBytes(StandardCharsets.UTF_8);
        String[] signatures = webhookSignature.split(" ");
        for (String sig : signatures) {
            if (MessageDigest.isEqual(expectedBytes, sig.trim().getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }
}
