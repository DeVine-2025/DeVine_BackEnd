package com.umc.devine.domain.maintenance.entity;

import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 서버 점검 모드 설정. 테이블에는 항상 id = 1인 단일 행만 존재한다(CHECK 제약으로 강제).
 */
@Entity
@Table(name = "maintenance_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MaintenanceSetting extends BaseEntity {

    public static final Long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "estimated_end_at")
    private LocalDateTime estimatedEndAt;

    /**
     * 점검 모드를 전환한다. 점검을 끌 때는 안내 메시지와 종료 예정 시각도 함께 비운다.
     */
    public void update(boolean enabled, String message, LocalDateTime estimatedEndAt) {
        this.enabled = enabled;
        this.message = enabled ? message : null;
        this.estimatedEndAt = enabled ? estimatedEndAt : null;
    }
}
