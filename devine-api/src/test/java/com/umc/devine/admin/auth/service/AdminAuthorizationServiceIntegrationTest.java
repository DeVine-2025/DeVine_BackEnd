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
 * (네이티브 ON CONFLICT upsert가 실제 스키마에서 동작하는지, 회수된 부트스트랩 관리자가 500 없이 거절되는지)
 */
@TestPropertySource(properties = "admin.bootstrap-emails=boot@devine.com,revoked@devine.com")
class AdminAuthorizationServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AdminAuthorizationService adminAuthorizationService;

    @Autowired
    private AdminRepository adminRepository;

    @Test
    @DisplayName("부트스트랩 이메일로 최초 접근하면 admin 테이블에 자동 등록되고 활성 관리자로 반환된다")
    void bootstrap_seeds_admin() {
        Optional<Admin> result = adminAuthorizationService.resolveAdmin("clerk_fresh", "boot@devine.com");

        assertThat(result).isPresent();
        assertThat(result.get().getLevel()).isEqualTo(AdminLevel.ADMIN);
        assertThat(result.get().isActive()).isTrue();

        Optional<Admin> persisted = adminRepository.findByClerkId("clerk_fresh");
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getGrantedBy()).isEqualTo("BOOTSTRAP");
    }

    @Test
    @DisplayName("회수된 관리자의 이메일이 부트스트랩 목록에 남아 있어도 재등록 없이 빈 Optional을 반환한다 (500 아님)")
    void revoked_bootstrap_admin_is_not_reseeded() {
        // given: 이메일은 부트스트랩 목록에 있지만 회수(비활성)된 관리자
        Admin revoked = adminRepository.save(Admin.builder()
                .clerkId("clerk_old")
                .email("revoked@devine.com")
                .build());
        revoked.deactivate();
        adminRepository.saveAndFlush(revoked);

        // when / then: 수정 전에는 UNIQUE 위반 → 트랜잭션 abort로 500이 나던 경로.
        // 예외 없이 빈 Optional이어야 한다.
        assertThatCode(() ->
                assertThat(adminAuthorizationService.resolveAdmin("clerk_new", "revoked@devine.com")).isEmpty()
        ).doesNotThrowAnyException();

        // 새 clerk_id로 재등록되지 않았다
        assertThat(adminRepository.findByClerkId("clerk_new")).isEmpty();
        // 기존 회수 행은 그대로 비활성
        assertThat(adminRepository.findByEmail("revoked@devine.com"))
                .get()
                .extracting(Admin::isActive)
                .isEqualTo(false);
    }

    @Test
    @DisplayName("관리자도 부트스트랩 이메일도 아니면 빈 Optional을 반환한다")
    void non_admin_returns_empty() {
        Optional<Admin> result = adminAuthorizationService.resolveAdmin("clerk_user", "user@devine.com");

        assertThat(result).isEmpty();
    }
}
