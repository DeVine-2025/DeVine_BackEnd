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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private AdminRepository adminRepository;

    private AdminAuthorizationService service(String bootstrapClerkIds) {
        return new AdminAuthorizationService(adminRepository, bootstrapClerkIds);
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
            given(adminRepository.findByClerkId("user_1"))
                    .willReturn(Optional.of(admin("user_1", "a@devine.com")));

            Optional<Admin> result = service("user_1").resolveAdmin("user_1", "a@devine.com");

            assertThat(result).isPresent();
            verifyNoSeeding();
        }

        @Test
        @DisplayName("회수(비활성)된 관리자는 부트스트랩 목록에 남아 있어도 빈 Optional을 반환한다 (회수 우선)")
        void revoked_admin_returns_empty() {
            given(adminRepository.findByClerkId("user_revoked"))
                    .willReturn(Optional.of(deactivatedAdmin("user_revoked", "boot@devine.com")));

            Optional<Admin> result = service("user_revoked").resolveAdmin("user_revoked", "boot@devine.com");

            assertThat(result).isEmpty();
            verifyNoSeeding();
        }
    }

    @Nested
    @DisplayName("부트스트랩 lazy seeding (clerk_id 기반)")
    class Bootstrap {

        @Test
        @DisplayName("부트스트랩 clerk_id로 최초 접근 시 자동 등록 후 관리자를 반환한다")
        void seeds_on_first_bootstrap_access() {
            given(adminRepository.findByClerkId("user_2"))
                    .willReturn(Optional.empty(), Optional.of(admin("user_2", "boot@devine.com")));

            Optional<Admin> result = service("user_2").resolveAdmin("user_2", "boot@devine.com");

            assertThat(result).isPresent();
            verify(adminRepository).insertBootstrapAdminIfAbsent(eq("user_2"), eq("boot@devine.com"), eq("BOOTSTRAP"));
        }

        @Test
        @DisplayName("email 클레임이 없어도(null) clerk_id만으로 자동 등록된다")
        void seeds_even_without_email() {
            given(adminRepository.findByClerkId("user_3"))
                    .willReturn(Optional.empty(), Optional.of(admin("user_3", null)));

            Optional<Admin> result = service("user_3").resolveAdmin("user_3", null);

            assertThat(result).isPresent();
            verify(adminRepository).insertBootstrapAdminIfAbsent(eq("user_3"), isNull(), eq("BOOTSTRAP"));
        }

        @Test
        @DisplayName("email은 정규화되어 저장된다(대소문자/공백)")
        void email_is_normalized_on_insert() {
            given(adminRepository.findByClerkId("user_4"))
                    .willReturn(Optional.empty(), Optional.of(admin("user_4", "boot@devine.com")));

            Optional<Admin> result = service("user_4").resolveAdmin("user_4", "  Boot@DEVINE.com ");

            assertThat(result).isPresent();
            verify(adminRepository).insertBootstrapAdminIfAbsent(eq("user_4"), eq("boot@devine.com"), eq("BOOTSTRAP"));
        }
    }

    @Nested
    @DisplayName("비관리자")
    class NonAdmin {

        @Test
        @DisplayName("부트스트랩 clerk_id도 아니고 기존 관리자도 아니면 빈 Optional을 반환한다")
        void returns_empty_for_non_admin() {
            given(adminRepository.findByClerkId("user_5")).willReturn(Optional.empty());

            Optional<Admin> result = service("user_2").resolveAdmin("user_5", "user@devine.com");

            assertThat(result).isEmpty();
            verifyNoSeeding();
        }

        @Test
        @DisplayName("부트스트랩 목록이 비어 있으면 아무도 자동 등록되지 않는다")
        void empty_bootstrap_grants_nobody() {
            given(adminRepository.findByClerkId("user_6")).willReturn(Optional.empty());

            Optional<Admin> result = service("").resolveAdmin("user_6", "user@devine.com");

            assertThat(result).isEmpty();
            verifyNoSeeding();
        }
    }
}