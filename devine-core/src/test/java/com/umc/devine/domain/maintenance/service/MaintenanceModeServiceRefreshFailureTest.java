package com.umc.devine.domain.maintenance.service;

import com.umc.devine.domain.maintenance.entity.MaintenanceSetting;
import com.umc.devine.domain.maintenance.repository.MaintenanceSettingRepository;
import com.umc.devine.support.CoreIntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

/**
 * DB 장애 중 refresh()가 어떻게 동작하는지 검증한다.
 * 실제 DB를 죽일 수 없으므로 이 케이스에 한해 리포지토리에 예외를 주입한다.
 */
class MaintenanceModeServiceRefreshFailureTest extends CoreIntegrationTestSupport {

    @Autowired
    private MaintenanceModeService maintenanceModeService;

    @MockitoSpyBean
    private MaintenanceSettingRepository maintenanceSettingRepository;

    @Test
    @DisplayName("refresh() 중 DB 장애가 나도 예외를 전파하지 않고 직전 캐시를 유지한다")
    void refresh_whenDatabaseFails_keepsPreviousCacheAndSwallowsException() {
        // given - 캐시를 점검 ON 상태로 만들어 둔다
        LocalDateTime endAt = LocalDateTime.of(2026, 7, 21, 18, 0);
        maintenanceModeService.update(true, "서버 점검 중입니다.", endAt);
        assertThat(maintenanceModeService.getState().enabled()).isTrue();

        // given - 이후 DB 조회는 실패한다
        doThrow(new DataAccessResourceFailureException("DB 연결 실패"))
                .when(maintenanceSettingRepository).findById(MaintenanceSetting.SINGLETON_ID);

        // when & then - 예외가 스케줄러로 전파되지 않는다
        assertThatCode(() -> maintenanceModeService.refresh()).doesNotThrowAnyException();

        // then - 점검 상태가 OFF로 초기화되지 않고 그대로 유지된다
        assertThat(maintenanceModeService.getState().enabled()).isTrue();
        assertThat(maintenanceModeService.getState().message()).isEqualTo("서버 점검 중입니다.");
        assertThat(maintenanceModeService.getState().estimatedEndAt()).isEqualTo(endAt);
    }
}
