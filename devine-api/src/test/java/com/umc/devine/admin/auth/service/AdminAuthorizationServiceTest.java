package com.umc.devine.admin.auth.service;

import com.umc.devine.admin.entity.Admin;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.admin.repository.AdminRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Nested
    @DisplayName("기존 관리자")
    class ExistingAdmin {

        @Test
        @DisplayName("admin 테이블에 활성 관리자가 있으면 그대로 반환하고 등록하지 않는다")
        void returns_existing_without_seeding() {
            // given
            given(adminRepository.findActiveByClerkId("clerk_1"))
                    .willReturn(Optional.of(admin("clerk_1", "a@devine.com")));

            // when
            Optional<Admin> result = service("a@devine.com").resolveAdmin("clerk_1", "a@devine.com");

            // then
            assertThat(result).isPresent();
            verify(adminRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("부트스트랩 lazy seeding")
    class Bootstrap {

        @Test
        @DisplayName("부트스트랩 이메일로 최초 접근 시 admin 테이블에 자동 등록한다")
        void seeds_on_first_bootstrap_access() {
            // given
            given(adminRepository.findActiveByClerkId("clerk_2")).willReturn(Optional.empty());
            given(adminRepository.findActiveByEmail("boot@devine.com")).willReturn(Optional.empty());
            given(adminRepository.save(any(Admin.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            Optional<Admin> result = service("boot@devine.com").resolveAdmin("clerk_2", "boot@devine.com");

            // then
            assertThat(result).isPresent();
            ArgumentCaptor<Admin> captor = ArgumentCaptor.forClass(Admin.class);
            verify(adminRepository).save(captor.capture());
            Admin saved = captor.getValue();
            assertThat(saved.getClerkId()).isEqualTo("clerk_2");
            assertThat(saved.getEmail()).isEqualTo("boot@devine.com");
            assertThat(saved.getLevel()).isEqualTo(AdminLevel.ADMIN);
        }

        @Test
        @DisplayName("부트스트랩 이메일 매칭은 대소문자/공백을 무시한다")
        void bootstrap_match_is_normalized() {
            // given
            given(adminRepository.findActiveByClerkId("clerk_3")).willReturn(Optional.empty());
            given(adminRepository.findActiveByEmail("boot@devine.com")).willReturn(Optional.empty());
            given(adminRepository.save(any(Admin.class))).willAnswer(inv -> inv.getArgument(0));

            // when: 설정은 대문자/공백 포함, 토큰 이메일도 대문자
            Optional<Admin> result = service("  BOOT@Devine.com ").resolveAdmin("clerk_3", "Boot@DEVINE.com");

            // then
            assertThat(result).isPresent();
            verify(adminRepository).save(any(Admin.class));
        }

        @Test
        @DisplayName("이미 같은 이메일의 관리자가 있으면 등록하지 않고 재사용한다")
        void reuses_when_email_already_registered() {
            // given
            given(adminRepository.findActiveByClerkId("clerk_4")).willReturn(Optional.empty());
            given(adminRepository.findActiveByEmail("boot@devine.com"))
                    .willReturn(Optional.of(admin("other_clerk", "boot@devine.com")));

            // when
            Optional<Admin> result = service("boot@devine.com").resolveAdmin("clerk_4", "boot@devine.com");

            // then
            assertThat(result).isPresent();
            verify(adminRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("비관리자")
    class NonAdmin {

        @Test
        @DisplayName("관리자도 아니고 부트스트랩 이메일도 아니면 빈 Optional을 반환한다")
        void returns_empty_for_non_admin() {
            // given
            given(adminRepository.findActiveByClerkId("clerk_5")).willReturn(Optional.empty());

            // when
            Optional<Admin> result = service("boot@devine.com").resolveAdmin("clerk_5", "user@devine.com");

            // then
            assertThat(result).isEmpty();
            verify(adminRepository, never()).save(any());
        }

        @Test
        @DisplayName("부트스트랩 목록이 비어 있으면 아무도 자동 등록되지 않는다")
        void empty_bootstrap_grants_nobody() {
            // given
            given(adminRepository.findActiveByClerkId("clerk_6")).willReturn(Optional.empty());

            // when
            Optional<Admin> result = service("").resolveAdmin("clerk_6", "user@devine.com");

            // then
            assertThat(result).isEmpty();
            verify(adminRepository, never()).save(any());
        }
    }
}
