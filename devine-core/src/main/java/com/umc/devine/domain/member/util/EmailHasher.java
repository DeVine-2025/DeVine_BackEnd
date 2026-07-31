package com.umc.devine.domain.member.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** 강제탈퇴자 재가입 제한 대조용 이메일 해시 유틸. 서버 비밀키(pepper)로 HMAC-SHA256 해시해 DB 유출 시에도 원본 역산을 어렵게 한다. */
@Component
public class EmailHasher {

    private final String pepper;

    public EmailHasher(@Value("${member.email-hash.secret}") String pepper) {
        if (pepper == null || pepper.isBlank()) {
            throw new IllegalStateException(
                    "member.email-hash.secret(MEMBER_EMAIL_HASH_SECRET)이 설정되지 않았습니다. " +
                    "빈 값으로는 HMAC 키를 생성할 수 없어 애플리케이션을 시작할 수 없습니다.");
        }
        this.pepper = pepper;
    }

    public String hash(String email) {
        String normalized = email.trim().toLowerCase();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hashed = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HmacSHA256 해시 계산에 실패했습니다.", e);
        }
    }
}
