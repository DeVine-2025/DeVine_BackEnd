package com.umc.devine.domain.maintenance.service;

import com.umc.devine.domain.maintenance.dto.MaintenanceState;
import com.umc.devine.domain.maintenance.entity.MaintenanceSetting;
import com.umc.devine.domain.maintenance.exception.MaintenanceException;
import com.umc.devine.domain.maintenance.exception.code.MaintenanceErrorReason;
import com.umc.devine.domain.maintenance.repository.MaintenanceSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 점검 모드 상태의 단일 소유자.
 *
 * <p>상태는 DB(단일행)가 원본이지만, 모든 요청이 거쳐 가는 값이므로 메모리에 캐시해 두고
 * 요청 처리 경로에서는 DB를 조회하지 않는다. 다른 인스턴스가 상태를 바꾼 경우는
 * {@link #refresh()} 주기 폴링으로 최대 {@value #REFRESH_INTERVAL_MS}ms 안에 전파된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceModeService {

    private static final long REFRESH_INTERVAL_MS = 10_000L;

    private final MaintenanceSettingRepository maintenanceSettingRepository;

    private volatile MaintenanceState cache;

    /**
     * 초기 로드는 실패 시 예외를 전파해 기동을 중단시킨다.
     * 점검 상태를 모르는 채 트래픽을 받는 것보다 뜨지 않는 편이 안전하다.
     *
     * <p>여기에 {@code @Transactional}을 붙이지 않는다. {@code @PostConstruct}는 AOP 프록시가
     * 적용되기 전, 원본 인스턴스에서 호출되므로 트랜잭션 어드바이스가 걸리지 않는다(무효).
     * 지금은 단일 읽기이고 Spring Data 리포지토리 호출이 자체 트랜잭션을 열어 문제가 없다.
     * <b>init에 쓰기나 다단계 DB 로직을 추가한다면</b> {@code @Transactional}로는 경계가 생기지
     * 않으니 {@code TransactionTemplate}으로 명시적 트랜잭션을 열어야 한다.
     */
    @PostConstruct
    public void init() {
        this.cache = load();
        log.info("점검 모드 초기 상태 로드 완료 - enabled: {}", cache.enabled());
    }

    public MaintenanceState getState() {
        return cache;
    }

    @Transactional
    public MaintenanceState update(boolean enabled, String message, LocalDateTime estimatedEndAt) {
        MaintenanceSetting setting = findSetting();
        setting.update(enabled, message, estimatedEndAt);
        maintenanceSettingRepository.save(setting);

        this.cache = MaintenanceState.from(setting);
        log.info("점검 모드 변경 - enabled: {}, estimatedEndAt: {}", enabled, estimatedEndAt);
        return cache;
    }

    /**
     * 다른 인스턴스가 변경한 상태를 캐시에 반영한다.
     *
     * <p>DB 장애 시 예외를 삼키고 직전 캐시를 유지한다. Spring이 반복 태스크의 예외를 이미
     * 로깅 후 무시하므로 스케줄러가 멈추지는 않지만, 장애가 길어지면 10초마다 스택트레이스가
     * 쌓여 정작 원인 로그를 덮는다. WARN 한 줄로 낮추고 "이전 값을 유지한다"는 의도를 드러낸다.
     */
    @Scheduled(fixedDelay = REFRESH_INTERVAL_MS)
    @Transactional(readOnly = true)
    public void refresh() {
        try {
            this.cache = load();
        } catch (Exception e) {
            log.warn("점검 모드 상태 갱신 실패 - 직전 캐시를 유지한다: {}", e.getMessage());
        }
    }

    private MaintenanceState load() {
        return MaintenanceState.from(findSetting());
    }

    private MaintenanceSetting findSetting() {
        return maintenanceSettingRepository.findById(MaintenanceSetting.SINGLETON_ID)
                .orElseThrow(() -> new MaintenanceException(MaintenanceErrorReason.SETTING_NOT_FOUND));
    }
}
