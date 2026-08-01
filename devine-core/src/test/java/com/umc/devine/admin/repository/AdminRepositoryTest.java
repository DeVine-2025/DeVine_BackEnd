package com.umc.devine.admin.repository;

import com.umc.devine.admin.entity.Admin;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AdminRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private AdminRepository adminRepository;

    private Admin saveAdmin(String clerkId, String email) {
        return adminRepository.save(Admin.builder()
                .clerkId(clerkId)
                .email(email)
                .build());
    }

    @Nested
    @DisplayName("엔티티 기본값")
    class Defaults {

        @Test
        @DisplayName("저장 시 level은 ADMIN, isActive는 true가 기본값이다")
        void defaults_applied() {
            Admin saved = saveAdmin("admin_clerk_1", "admin1@devine.com");

            assertThat(saved.getLevel()).isEqualTo(AdminLevel.ADMIN);
            assertThat(saved.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("findByClerkId (활성 여부 무관)")
    class Finders {

        @Test
        @DisplayName("활성 관리자를 clerkId로 조회한다")
        void finds_active_admin() {
            saveAdmin("admin_clerk_2", "admin2@devine.com");

            assertThat(adminRepository.findByClerkId("admin_clerk_2")).isPresent();
        }

        @Test
        @DisplayName("비활성(회수) 관리자도 조회된다 (회수 우선 판정을 위해 활성 여부를 필터링하지 않는다)")
        void finds_deactivated_admin() {
            Admin admin = saveAdmin("admin_clerk_3", "admin3@devine.com");
            admin.deactivate();
            adminRepository.save(admin);

            Optional<Admin> byClerkId = adminRepository.findByClerkId("admin_clerk_3");
            assertThat(byClerkId).isPresent();
            assertThat(byClerkId.get().isActive()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않으면 빈 Optional을 반환한다")
        void returns_empty_when_absent() {
            assertThat(adminRepository.findByClerkId("no_such_clerk")).isEmpty();
        }
    }

    @Nested
    @DisplayName("insertBootstrapAdminIfAbsent (ON CONFLICT DO NOTHING)")
    class BootstrapUpsert {

        @Test
        @DisplayName("행이 없으면 기본값(level=ADMIN, is_active=true)으로 삽입한다")
        void inserts_with_defaults() {
            adminRepository.insertBootstrapAdminIfAbsent("boot_clerk_1", "boot1@devine.com", "BOOTSTRAP");

            Optional<Admin> result = adminRepository.findByClerkId("boot_clerk_1");
            assertThat(result).isPresent();
            assertThat(result.get().getLevel()).isEqualTo(AdminLevel.ADMIN);
            assertThat(result.get().isActive()).isTrue();
            assertThat(result.get().getGrantedBy()).isEqualTo("BOOTSTRAP");
        }

        @Test
        @DisplayName("email이 null이어도 삽입된다 (email은 nullable)")
        void inserts_with_null_email() {
            assertThatCode(() ->
                    adminRepository.insertBootstrapAdminIfAbsent("boot_clerk_null", null, "BOOTSTRAP")
            ).doesNotThrowAnyException();

            Optional<Admin> result = adminRepository.findByClerkId("boot_clerk_null");
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isNull();
        }

        @Test
        @DisplayName("같은 clerk_id로 다시 호출해도 예외 없이 멱등하다 (clerk_id 충돌)")
        void idempotent_on_clerk_id_conflict() {
            adminRepository.insertBootstrapAdminIfAbsent("boot_clerk_2", "boot2@devine.com", "BOOTSTRAP");

            assertThatCode(() ->
                    adminRepository.insertBootstrapAdminIfAbsent("boot_clerk_2", "boot2@devine.com", "BOOTSTRAP")
            ).doesNotThrowAnyException();

            assertThat(adminRepository.findByClerkId("boot_clerk_2")).isPresent();
        }

        @Test
        @DisplayName("다른 clerk_id + 같은 email로 호출해도 예외 없이 no-op이다 (email 충돌)")
        void idempotent_on_email_conflict() {
            adminRepository.insertBootstrapAdminIfAbsent("boot_clerk_3", "shared@devine.com", "BOOTSTRAP");

            assertThatCode(() ->
                    adminRepository.insertBootstrapAdminIfAbsent("boot_clerk_other", "shared@devine.com", "BOOTSTRAP")
            ).doesNotThrowAnyException();

            // email 충돌로 두 번째(다른 clerk_id)는 삽입되지 않고, 첫 행은 그대로 존재한다
            assertThat(adminRepository.findByClerkId("boot_clerk_other")).isEmpty();
            assertThat(adminRepository.findByClerkId("boot_clerk_3")).isPresent();
        }
    }
}