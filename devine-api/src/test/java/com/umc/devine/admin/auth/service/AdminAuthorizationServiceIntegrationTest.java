package com.umc.devine.admin.auth.service;

import com.umc.devine.admin.entity.Admin;
import com.umc.devine.admin.enums.AdminLevel;
import com.umc.devine.admin.repository.AdminRepository;
import com.umc.devine.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 실제 Postgres에 대해 AdminAuthorizationService의 seeding/회수 동작을 검증한다.
 * (네이티브 ON CONFLICT upsert가 실제 스키마에서 동작하는지, 회수된 관리자가 500 없이 거절되는지)
 */
@TestPropertySource(properties = "admin.bootstrap-clerk-ids=user_boot,user_revoked")
class AdminAuthorizationServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AdminAuthorizationService adminAuthorizationService;

    @Autowired
    private AdminRepository adminRepository;

    @Test
    @DisplayName("부트스트랩 clerk_id로 최초 접근하면 admin 테이블에 자동 등록되고 활성 관리자로 반환된다")
    void bootstrap_seeds_admin() {
        Optional<Admin> result = adminAuthorizationService.resolveAdmin("user_boot", "boot@devine.com");

        assertThat(result).isPresent();
        assertThat(result.get().getLevel()).isEqualTo(AdminLevel.ADMIN);
        assertThat(result.get().isActive()).isTrue();

        Optional<Admin> persisted = adminRepository.findByClerkId("user_boot");
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getGrantedBy()).isEqualTo("BOOTSTRAP");
        assertThat(persisted.get().getEmail()).isEqualTo("boot@devine.com");
    }

    @Test
    @DisplayName("email 클레임이 없어도(null) 부트스트랩 clerk_id면 등록된다")
    void bootstrap_seeds_without_email() {
        Optional<Admin> result = adminAuthorizationService.resolveAdmin("user_boot", null);

        assertThat(result).isPresent();
        assertThat(adminRepository.findByClerkId("user_boot"))
                .get().extracting(Admin::getEmail).isNull();
    }

    @Test
    @DisplayName("부트스트랩 clerk_id가 아니면 빈 Optional을 반환한다")
    void non_bootstrap_returns_empty() {
        Optional<Admin> result = adminAuthorizationService.resolveAdmin("user_stranger", "stranger@devine.com");

        assertThat(result).isEmpty();
        assertThat(adminRepository.findByClerkId("user_stranger")).isEmpty();
    }

    @Test
    @DisplayName("회수된 관리자는 부트스트랩 목록에 clerk_id가 남아 있어도 재등록 없이 빈 Optional을 반환한다 (500 아님)")
    void revoked_bootstrap_admin_is_not_reseeded() {
        // given: 부트스트랩 목록에 있는 clerk_id지만 회수(비활성)된 관리자
        Admin revoked = adminRepository.save(Admin.builder()
                .clerkId("user_revoked")
                .email("revoked@devine.com")
                .build());
        revoked.deactivate();
        adminRepository.saveAndFlush(revoked);

        // when / then: 예외 없이 빈 Optional (수정 전에는 UNIQUE 위반 → abort → 500이던 경로)
        assertThatCode(() ->
                assertThat(adminAuthorizationService.resolveAdmin("user_revoked", "revoked@devine.com")).isEmpty()
        ).doesNotThrowAnyException();

        // 여전히 비활성 1건, 중복 등록 없음
        assertThat(adminRepository.findByClerkId("user_revoked"))
                .get().extracting(Admin::isActive).isEqualTo(false);
    }
}