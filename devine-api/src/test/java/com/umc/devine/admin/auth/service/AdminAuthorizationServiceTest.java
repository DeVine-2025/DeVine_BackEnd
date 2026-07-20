package com.umc.devine.admin.auth.service;

import com.umc.devine.admin.entity.Admin;
import com.umc.devine.admin.repository.AdminRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private AdminRepository adminRepository;

    private AdminAuthorizationService service(String bootstrapEmails) {
        return new AdminAuthorizationService(adminRepository, bootstrapEmails);
    }

    private Admin admin(String clerkId, String email) {
        return Admin.builder().clerkId(clerkId).email(email).build();
    }

    private Admin deactivatedAdmin(String clerkId, String email) {
        Admin admin = admin(clerkId, email);
        admin.deactivate();
        return admin;
    }

    private void verifyNoSeeding() {
        verify(adminRepository, never()).insertBootstrapAdminIfAbsent(any(), any(), any());
    }

    @Nested
    @DisplayName("기존 관리자")
    class ExistingAdmin {

        @Test
        @DisplayName("활성 관리자가 clerkId로 존재하면 그대로 반환하고 등록하지 않는다")
        void returns_existing_active() {
            given(adminRepository.findByClerkId("clerk_1"))
                    .willReturn(Optional.of(admin("clerk_1", "a@devine.com")));

            Optional<Admin> result = service("a@devine.com").resolveAdmin("clerk_1", "a@devine.com");

            assertThat(result).isPresent();
            verifyNoSeeding();
        }

        @Test
        @DisplayName("회수(비활성)된 관리자는 부트스트랩 목록에 남아 있어도 빈 Optional을 반환한다 (회수 우선)")
        void revoked_admin_by_clerk_id_returns_empty() {
            given(adminRepository.findByClerkId("clerk_revoked"))
                    .willReturn(Optional.of(deactivatedAdmin("clerk_revoked", "boot@devine.com")));

            Optional<Admin> result = service("boot@devine.com").resolveAdmin("clerk_revoked", "boot@devine.com");

            assertThat(result).isEmpty();
            verifyNoSeeding();
        }
    }

    @Nested
    @DisplayName("부트스트랩 lazy seeding")
    class Bootstrap {

        @Test
        @DisplayName("부트스트랩 이메일로 최초 접근 시 자동 등록 후 관리자를 반환한다")
        void seeds_on_first_bootstrap_access() {
            given(adminRepository.findByClerkId("clerk_2"))
                    .willReturn(Optional.empty(), Optional.of(admin("clerk_2", "boot@devine.com")));
            given(adminRepository.findByEmail("boot@devine.com")).willReturn(Optional.empty());

            Optional<Admin> result = service("boot@devine.com").resolveAdmin("clerk_2", "boot@devine.com");

            assertThat(result).isPresent();
            verify(adminRepository).insertBootstrapAdminIfAbsent("clerk_2", "boot@devine.com", "BOOTSTRAP");
        }

        @Test
        @DisplayName("부트스트랩 이메일 매칭과 등록은 대소문자/공백을 정규화한다")
        void bootstrap_match_and_insert_are_normalized() {
            given(adminRepository.findByClerkId("clerk_3"))
                    .willReturn(Optional.empty(), Optional.of(admin("clerk_3", "boot@devine.com")));
            given(adminRepository.findByEmail("boot@devine.com")).willReturn(Optional.empty());

            // 설정과 토큰 이메일 모두 대문자/공백 포함
            Optional<Admin> result = service("  BOOT@Devine.com ").resolveAdmin("clerk_3", "Boot@DEVINE.com");

            assertThat(result).isPresent();
            verify(adminRepository).insertBootstrapAdminIfAbsent(eq("clerk_3"), eq("boot@devine.com"), eq("BOOTSTRAP"));
        }

        @Test
        @DisplayName("이미 같은 이메일의 활성 관리자가 있으면 등록하지 않고 재사용한다")
        void reuses_when_active_email_exists() {
            given(adminRepository.findByClerkId("clerk_4")).willReturn(Optional.empty());
            given(adminRepository.findByEmail("boot@devine.com"))
                    .willReturn(Optional.of(admin("other_clerk", "boot@devine.com")));

            Optional<Admin> result = service("boot@devine.com").resolveAdmin("clerk_4", "boot@devine.com");

            assertThat(result).isPresent();
            verifyNoSeeding();
        }

        @Test
        @DisplayName("같은 이메일의 관리자가 회수(비활성)되어 있으면 재등록하지 않고 빈 Optional을 반환한다")
        void revoked_admin_by_email_returns_empty() {
            given(adminRepository.findByClerkId("clerk_5")).willReturn(Optional.empty());
            given(adminRepository.findByEmail("boot@devine.com"))
                    .willReturn(Optional.of(deactivatedAdmin("old_clerk", "boot@devine.com")));

            Optional<Admin> result = service("boot@devine.com").resolveAdmin("clerk_5", "boot@devine.com");

            assertThat(result).isEmpty();
            verifyNoSeeding();
        }
    }

    @Nested
    @DisplayName("비관리자")
    class NonAdmin {

        @Test
        @DisplayName("관리자도 아니고 부트스트랩 이메일도 아니면 빈 Optional을 반환한다")
        void returns_empty_for_non_admin() {
            given(adminRepository.findByClerkId("clerk_6")).willReturn(Optional.empty());

            Optional<Admin> result = service("boot@devine.com").resolveAdmin("clerk_6", "user@devine.com");

            assertThat(result).isEmpty();
            verifyNoSeeding();
        }

        @Test
        @DisplayName("부트스트랩 목록이 비어 있으면 아무도 자동 등록되지 않는다")
        void empty_bootstrap_grants_nobody() {
            given(adminRepository.findByClerkId("clerk_7")).willReturn(Optional.empty());

            Optional<Admin> result = service("").resolveAdmin("clerk_7", "user@devine.com");

            assertThat(result).isEmpty();
            verifyNoSeeding();
        }
    }
}