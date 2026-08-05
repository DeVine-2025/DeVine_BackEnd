package com.umc.devine.admin.integration.converter;

import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;
import com.umc.devine.admin.integration.entity.ExternalIntegrationHealth;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class AdminIntegrationConverter {

    public static AdminIntegrationResDTO.IntegrationHealthDTO toIntegrationHealthDTO(ExternalIntegrationHealth health) {
        return new AdminIntegrationResDTO.IntegrationHealthDTO(
                health.getIntegrationType(),
                health.getIntegrationType().getDisplayName(),
                health.getStatus(),
                health.getStatus().getLabel(),
                health.getResponseTimeMs(),
                health.getCheckedAt(),
                health.getErrorMessage()
        );
    }

    public static AdminIntegrationResDTO.HealthSummaryDTO toHealthSummaryDTO(List<ExternalIntegrationHealth> healths) {
        List<AdminIntegrationResDTO.IntegrationHealthDTO> integrations = healths.stream()
                .map(AdminIntegrationConverter::toIntegrationHealthDTO)
                .toList();

        // 전체 스냅샷의 신선도는 가장 오래 갱신되지 않은 항목이 결정한다
        LocalDateTime oldestCheckedAt = healths.stream()
                .map(ExternalIntegrationHealth::getCheckedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new AdminIntegrationResDTO.HealthSummaryDTO(oldestCheckedAt, integrations);
    }
}
