package com.umc.devine.admin.integration.service.query;

import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;

public interface AdminIntegrationQueryService {

    /**
     * 저장된 최신 스냅샷을 조회한다. 외부 호출을 하지 않는다.
     */
    AdminIntegrationResDTO.HealthSummaryDTO getHealthSnapshot();
}
