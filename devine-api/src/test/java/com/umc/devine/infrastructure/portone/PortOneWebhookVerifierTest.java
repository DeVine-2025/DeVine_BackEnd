package com.umc.devine.infrastructure.portone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class PortOneWebhookVerifierTest {

    // 테스트용 시크릿 (whsec_ 접두사 + Base64)
    private static final byte[] TEST_SECRET = "test-webhook-secret-key!".getBytes(StandardCharsets.UTF_8);
    private static final String TEST_WEBHOOK_SECRET = "whsec_" + Base64.getEncoder().encodeToString(TEST_SECRET);

    private final PortOneWebhookVerifier verifier = new PortOneWebhookVerifier(TEST_WEBHOOK_SECRET);

    @Test
    @DisplayName("올바른 서명이면 검증 성공")
    void verify_validSignature_returnsTrue() throws Exception {
        String webhookId = "msg_test123";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String body = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"payment_123\"}}";

        String signature = generateSignature(webhookId, timestamp, body);

        assertThat(verifier.verify(webhookId, timestamp, signature, body)).isTrue();
    }

    @Test
    @DisplayName("잘못된 서명이면 검증 실패")
    void verify_invalidSignature_returnsFalse() {
        String webhookId = "msg_test123";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String body = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"payment_123\"}}";

        assertThat(verifier.verify(webhookId, timestamp, "v1,invalidsignature==", body)).isFalse();
    }

    @Test
    @DisplayName("타임스탬프가 5분 초과면 검증 실패 (리플레이 공격 방지)")
    void verify_expiredTimestamp_returnsFalse() throws Exception {
        String webhookId = "msg_test123";
        String timestamp = String.valueOf((System.currentTimeMillis() / 1000) - 600); // 10분 전
        String body = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"payment_123\"}}";

        String signature = generateSignature(webhookId, timestamp, body);

        assertThat(verifier.verify(webhookId, timestamp, signature, body)).isFalse();
    }

    @Test
    @DisplayName("본문이 변조되면 검증 실패")
    void verify_tamperedBody_returnsFalse() throws Exception {
        String webhookId = "msg_test123";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String originalBody = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"payment_123\"}}";
        String tamperedBody = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"payment_999\"}}";

        String signature = generateSignature(webhookId, timestamp, originalBody);

        assertThat(verifier.verify(webhookId, timestamp, signature, tamperedBody)).isFalse();
    }

    @Test
    @DisplayName("여러 서명 중 하나가 일치하면 검증 성공")
    void verify_multipleSignatures_oneValid_returnsTrue() throws Exception {
        String webhookId = "msg_test123";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String body = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"payment_123\"}}";

        String validSignature = generateSignature(webhookId, timestamp, body);
        String multiSignatures = "v1,oldsignature== " + validSignature;

        assertThat(verifier.verify(webhookId, timestamp, multiSignatures, body)).isTrue();
    }

    @Test
    @DisplayName("잘못된 타임스탬프 포맷이면 검증 실패")
    void verify_invalidTimestampFormat_returnsFalse() throws Exception {
        assertThat(verifier.verify("msg_test", "not-a-number", "v1,sig==", "body")).isFalse();
    }

    private String generateSignature(String webhookId, String timestamp, String body) throws Exception {
        String signedContent = webhookId + "." + timestamp + "." + body;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET, "HmacSHA256"));
        byte[] hash = mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8));
        return "v1," + Base64.getEncoder().encodeToString(hash);
    }
}
