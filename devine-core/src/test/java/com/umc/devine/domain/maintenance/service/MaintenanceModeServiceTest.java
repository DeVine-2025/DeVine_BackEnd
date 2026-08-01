package com.umc.devine.domain.maintenance.service;

import com.umc.devine.domain.maintenance.dto.MaintenanceState;
import com.umc.devine.support.CoreIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceModeServiceTest extends CoreIntegrationTestSupport {

    @Autowired
    private MaintenanceModeService maintenanceModeService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 서비스 캐시는 싱글턴 빈이 들고 있어 테스트의 트랜잭션 롤백을 따라가지 않는다.
     * 각 테스트가 앞 테스트의 캐시를 물려받지 않도록 DB(롤백된 초기 상태)로 되돌린다.
     */
    @BeforeEach
    void syncCacheWithDatabase() {
        maintenanceModeService.refresh();
    }

    /** 캐시를 우회해 DB만 직접 점검 ON으로 바꾼다. */
    private void enableInDatabaseOnly() {
        entityManager.createNativeQuery(
                "UPDATE maintenance_setting SET enabled = true, message = 'DB에서 직접 변경' WHERE id = 1"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("기동 시 DB에서 상태를 읽어 캐시에 담는다")
    void init_loadsStateFromDatabase() {
        // when
        MaintenanceState state = maintenanceModeService.getState();

        // then
        assertThat(state).isNotNull();
        assertThat(state.enabled()).isFalse();
    }

    @Test
    @DisplayName("점검 모드를 켜면 DB에 저장되고 캐시도 즉시 갱신된다")
    void update_persistsAndRefreshesCacheImmediately() {
        // given
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 21, 18, 0);

        // when
        maintenanceModeService.update(true, "서버 점검 중입니다.", endAt);

        // then - 캐시가 즉시 갱신됨
        MaintenanceState cached = maintenanceModeService.getState();
        assertThat(cached.enabled()).isTrue();
        assertThat(cached.message()).isEqualTo("서버 점검 중입니다.");
        assertThat(cached.estimatedEndAt()).isEqualTo(endAt);

        // then - DB에도 반영됨 (clear() 전에 flush 해야 변경분이 버려지지 않는다)
        entityManager.flush();
        entityManager.clear();
        Boolean enabledInDb = (Boolean) entityManager.createNativeQuery(
                "SELECT enabled FROM maintenance_setting WHERE id = 1"
        ).getSingleResult();
        assertThat(enabledInDb).isTrue();
    }

    @Test
    @DisplayName("getState()는 DB를 다시 조회하지 않는다 - DB가 바뀌어도 캐시 값을 반환한다")
    void getState_readsFromCacheNotDatabase() {
        // given
        enableInDatabaseOnly();

        // when
        MaintenanceState state = maintenanceModeService.getState();

        // then - refresh 전이므로 여전히 예전 값
        assertThat(state.enabled()).isFalse();
    }

    @Test
    @DisplayName("refresh()가 DB의 변경 사항을 캐시에 반영한다 - 다중 인스턴스 전파 경로")
    void refresh_picksUpExternalChange() {
        // given
        enableInDatabaseOnly();
        assertThat(maintenanceModeService.getState().enabled()).isFalse();

        // when
        maintenanceModeService.refresh();

        // then
        MaintenanceState state = maintenanceModeService.getState();
        assertThat(state.enabled()).isTrue();
        assertThat(state.message()).isEqualTo("DB에서 직접 변경");
    }
}
