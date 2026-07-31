package com.umc.devine.domain.member.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailHasherTest {

    @Test
    @DisplayName("pepper가 비어있으면 생성 시점에 즉시 실패한다")
    void constructor_blankPepper_throwsImmediately() {
        assertThatThrownBy(() -> new EmailHasher(""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EmailHasher(null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new EmailHasher("   "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("같은 이메일은 항상 같은 해시를 반환한다")
    void hash_sameEmail_producesSameHash() {
        EmailHasher hasher = new EmailHasher("test-pepper");

        String hash1 = hasher.hash("user@example.com");
        String hash2 = hasher.hash("user@example.com");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("대소문자와 앞뒤 공백을 무시하고 동일하게 취급한다")
    void hash_normalizesEmail() {
        EmailHasher hasher = new EmailHasher("test-pepper");

        String hash1 = hasher.hash("User@Example.com");
        String hash2 = hasher.hash("  user@example.com  ");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 이메일은 다른 해시를 반환한다")
    void hash_differentEmail_producesDifferentHash() {
        EmailHasher hasher = new EmailHasher("test-pepper");

        String hash1 = hasher.hash("user1@example.com");
        String hash2 = hasher.hash("user2@example.com");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("pepper가 다르면 같은 이메일이라도 다른 해시가 나온다")
    void hash_differentPepper_producesDifferentHash() {
        EmailHasher hasher1 = new EmailHasher("pepper-a");
        EmailHasher hasher2 = new EmailHasher("pepper-b");

        assertThat(hasher1.hash("user@example.com")).isNotEqualTo(hasher2.hash("user@example.com"));
    }
}
