package com.umc.devine.domain.maintenance.repository;

import com.umc.devine.domain.maintenance.entity.MaintenanceSetting;
import com.umc.devine.support.CoreIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaintenanceSettingRepositoryTest extends CoreIntegrationTestSupport {

    @Autowired
    private MaintenanceSettingRepository maintenanceSettingRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("마이그레이션이 점검 비활성 상태의 초기행을 생성해 둔다")
    void initialRow_exists_andDisabled() {
        // when
        Optional<MaintenanceSetting> result =
                maintenanceSettingRepository.findById(MaintenanceSetting.SINGLETON_ID);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().isEnabled()).isFalse();
        assertThat(result.get().getMessage()).isNull();
        assertThat(result.get().getEstimatedEndAt()).isNull();
    }

    @Test
    @DisplayName("점검 모드 전환 내용이 저장된다")
    void update_persisted() {
        // given
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 21, 18, 0);
        MaintenanceSetting setting =
                maintenanceSettingRepository.findById(MaintenanceSetting.SINGLETON_ID).orElseThrow();

        // when
        setting.update(true, "서버 점검 중입니다.", endAt);
        maintenanceSettingRepository.saveAndFlush(setting);
        entityManager.clear();

        // then
        MaintenanceSetting reloaded =
                maintenanceSettingRepository.findById(MaintenanceSetting.SINGLETON_ID).orElseThrow();
        assertThat(reloaded.isEnabled()).isTrue();
        assertThat(reloaded.getMessage()).isEqualTo("서버 점검 중입니다.");
        assertThat(reloaded.getEstimatedEndAt()).isEqualTo(endAt);
    }

    @Test
    @DisplayName("점검 모드를 끄면 안내 메시지와 종료 예정 시각이 함께 비워진다")
    void update_disable_clearsMessageAndEndAt() {
        // given
        MaintenanceSetting setting =
                maintenanceSettingRepository.findById(MaintenanceSetting.SINGLETON_ID).orElseThrow();
        setting.update(true, "서버 점검 중입니다.", LocalDateTime.of(2026, 7, 21, 18, 0));
        maintenanceSettingRepository.saveAndFlush(setting);

        // when
        setting.update(false, null, null);
        maintenanceSettingRepository.saveAndFlush(setting);
        entityManager.clear();

        // then
        MaintenanceSetting reloaded =
                maintenanceSettingRepository.findById(MaintenanceSetting.SINGLETON_ID).orElseThrow();
        assertThat(reloaded.isEnabled()).isFalse();
        assertThat(reloaded.getMessage()).isNull();
        assertThat(reloaded.getEstimatedEndAt()).isNull();
    }

    @Test
    @DisplayName("id가 1이 아닌 행은 CHECK 제약으로 삽입할 수 없다 - 설정은 항상 하나뿐")
    void secondRow_rejected() {
        // when & then
        assertThatThrownBy(() -> {
            entityManager.createNativeQuery(
                    "INSERT INTO maintenance_setting (id, enabled, created_at) VALUES (2, false, now())"
            ).executeUpdate();
            entityManager.flush();
        })
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("maintenance_setting_singleton");
    }
}
