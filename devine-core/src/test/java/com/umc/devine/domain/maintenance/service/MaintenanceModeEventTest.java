package com.umc.devine.domain.maintenance.service;

import com.umc.devine.domain.maintenance.event.MaintenanceEnabledEvent;
import com.umc.devine.support.CoreIntegrationTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 점검 모드가 OFF→ON으로 전환될 때만 {@link MaintenanceEnabledEvent}가 발행되는지 검증한다.
 * 이 이벤트를 realtime 프로세스가 받아 기존 SSE 연결을 끊는다.
 */
@RecordApplicationEvents
class MaintenanceModeEventTest extends CoreIntegrationTestSupport {

    @Autowired
    private MaintenanceModeService maintenanceModeService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void syncCacheToOff() {
        // 캐시를 DB(롤백된 OFF)로 되돌린다. ON→OFF거나 OFF→OFF라 이벤트를 발행하지 않는다.
        maintenanceModeService.refresh();
    }

    private long enabledEventCount() {
        return applicationEvents.stream(MaintenanceEnabledEvent.class).count();
    }

    @Test
    @DisplayName("점검을 켜면 MaintenanceEnabledEvent가 1회 발행된다")
    void update_enable_publishesEvent() {
        maintenanceModeService.update(true, "점검 중", null);

        assertThat(enabledEventCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("점검을 끄면 이벤트를 발행하지 않는다")
    void update_disable_doesNotPublish() {
        maintenanceModeService.update(false, null, null);

        assertThat(enabledEventCount()).isZero();
    }

    @Test
    @DisplayName("이미 켜진 상태에서 다시 켜도 중복 발행하지 않는다")
    void update_alreadyEnabled_doesNotRepublish() {
        maintenanceModeService.update(true, "점검 중", null);
        maintenanceModeService.update(true, "점검 연장", null);

        assertThat(enabledEventCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 프로세스가 DB를 켠 것을 refresh()가 감지하면 이벤트를 발행한다")
    void refresh_detectsExternalEnable_publishesEvent() {
        entityManager.createNativeQuery(
                "UPDATE maintenance_setting SET enabled = true WHERE id = 1"
        ).executeUpdate();
        entityManager.flush();
        entityManager.clear();

        maintenanceModeService.refresh();

        assertThat(enabledEventCount()).isEqualTo(1);
    }
}
