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
            // when
            Admin saved = saveAdmin("admin_clerk_1", "admin1@devine.com");

            // then
            assertThat(saved.getLevel()).isEqualTo(AdminLevel.ADMIN);
            assertThat(saved.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("findActiveByClerkId")
    class FindActiveByClerkId {

        @Test
        @DisplayName("활성 관리자를 clerkId로 조회한다")
        void returns_active_admin() {
            // given
            saveAdmin("admin_clerk_2", "admin2@devine.com");

            // when
            Optional<Admin> result = adminRepository.findActiveByClerkId("admin_clerk_2");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("admin2@devine.com");
        }

        @Test
        @DisplayName("비활성(회수) 관리자는 조회되지 않는다")
        void ignores_deactivated_admin() {
            // given
            Admin admin = saveAdmin("admin_clerk_3", "admin3@devine.com");
            admin.deactivate();
            adminRepository.save(admin);

            // when
            Optional<Admin> result = adminRepository.findActiveByClerkId("admin_clerk_3");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 clerkId면 빈 Optional을 반환한다")
        void returns_empty_when_absent() {
            // when
            Optional<Admin> result = adminRepository.findActiveByClerkId("no_such_clerk");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveByEmail")
    class FindActiveByEmail {

        @Test
        @DisplayName("활성 관리자를 email로 조회한다 (부트스트랩용)")
        void returns_active_admin_by_email() {
            // given
            saveAdmin("admin_clerk_4", "admin4@devine.com");

            // when
            Optional<Admin> result = adminRepository.findActiveByEmail("admin4@devine.com");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getClerkId()).isEqualTo("admin_clerk_4");
        }
    }
}