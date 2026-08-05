package com.umc.devine.admin.integration.entity;

import com.umc.devine.admin.integration.enums.IntegrationStatus;
import com.umc.devine.admin.integration.enums.IntegrationType;
import com.umc.devine.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 외부 연동별 최신 헬스체크 스냅샷. 연동 하나당 한 행만 유지된다(integration_type UNIQUE).
 * 이력은 보관하지 않고 점검할 때마다 덮어쓴다.
 */
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "external_integration_health")
public class ExternalIntegrationHealth extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "external_integration_health_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "integration_type", nullable = false, length = 30, unique = true)
    private IntegrationType integrationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IntegrationStatus status;

    /** 응답을 받지 못한 경우(타임아웃, 설정값 누락 등) null */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    public void updateResult(IntegrationStatus status, Long responseTimeMs, LocalDateTime checkedAt, String errorMessage) {
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.checkedAt = checkedAt;
        this.errorMessage = errorMessage;
    }
}
