package com.umc.devine.domain.member.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailHasherTest {

    @Test
    @DisplayName("소문자/공백 정리 후 HMAC-SHA256 hex(64자)로 해시하고, 같은 이메일은 같은 값이다")
    void normalizesAndHashes() {
        EmailHasher hasher = new EmailHasher("pepper", false);

        String hash = hasher.hash(" User@Example.COM ").orElseThrow();

        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
        assertThat(hasher.hash("user@example.com")).contains(hash);
        assertThat(hasher.hash("other@example.com")).isNotEqualTo(hasher.hash("user@example.com"));
    }

    @Test
    @DisplayName("pepper가 다르면 해시도 다르다(pepper 없이 이메일을 대입해 찾을 수 없다)")
    void pepperChangesHash() {
        assertThat(new EmailHasher("a", false).hash("user@example.com"))
                .isNotEqualTo(new EmailHasher("b", false).hash("user@example.com"));
    }

    @Test
    @DisplayName("pepper나 이메일이 없으면 해시를 만들지 않는다")
    void emptyWithoutPepperOrEmail() {
        assertThat(new EmailHasher("", false).hash("user@example.com")).isEmpty();
        assertThat(new EmailHasher("pepper", false).hash(null)).isEmpty();
        assertThat(new EmailHasher("pepper", false).hash(" ")).isEmpty();
    }

    @Test
    @DisplayName("플래그를 켰는데 pepper가 없으면 앱 시작을 막는다")
    void failFastWhenFlagOnWithoutPepper() {
        assertThatThrownBy(() -> new EmailHasher("", true).validate())
                .isInstanceOf(IllegalStateException.class);
    }
}
