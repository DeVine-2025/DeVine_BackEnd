package com.umc.devine.admin.integration.service.command;

import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;

public interface AdminIntegrationCommandService {

    /**
     * 등록된 모든 프로브를 병렬 실행해 상태를 판정하고 저장한 뒤 결과를 반환한다.
     * 스케줄러와 관리자 새로고침 요청이 동일하게 이 메서드를 사용한다.
     */
    AdminIntegrationResDTO.HealthSummaryDTO refreshAll();
}
