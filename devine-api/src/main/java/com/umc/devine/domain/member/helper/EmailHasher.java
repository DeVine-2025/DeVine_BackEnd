package com.umc.devine.domain.member.helper;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Component
public class EmailHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final String pepper;
    private final boolean retainSanctionedMember;

    public EmailHasher(
            @Value("${withdrawal.email-hash-pepper:}") String pepper,
            @Value("${withdrawal.retain-sanctioned-member:false}") boolean retainSanctionedMember
    ) {
        this.pepper = pepper;
        this.retainSanctionedMember = retainSanctionedMember;
    }

    @PostConstruct
    void validate() {
        if (retainSanctionedMember && !StringUtils.hasText(pepper)) {
            throw new IllegalStateException(
                    "withdrawal.retain-sanctioned-member=true이면 withdrawal.email-hash-pepper(WITHDRAWAL_EMAIL_HASH_PEPPER)가 필요합니다.");
        }
    }

    public Optional<String> hash(String email) {
        if (!StringUtils.hasText(pepper) || !StringUtils.hasText(email)) {
            return Optional.empty();
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return Optional.of(HexFormat.of().formatHex(digest));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("이메일 해시 생성 실패", e);
        }
    }
}
