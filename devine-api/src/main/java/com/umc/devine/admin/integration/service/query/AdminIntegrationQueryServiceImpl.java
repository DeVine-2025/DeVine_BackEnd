package com.umc.devine.admin.integration.service.query;

import com.umc.devine.admin.integration.converter.AdminIntegrationConverter;
import com.umc.devine.admin.integration.dto.AdminIntegrationResDTO;
import com.umc.devine.admin.integration.entity.ExternalIntegrationHealth;
import com.umc.devine.admin.integration.repository.ExternalIntegrationHealthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminIntegrationQueryServiceImpl implements AdminIntegrationQueryService {

    private final ExternalIntegrationHealthRepository externalIntegrationHealthRepository;

    @Override
    public AdminIntegrationResDTO.HealthSummaryDTO getHealthSnapshot() {
        List<ExternalIntegrationHealth> healths =
                externalIntegrationHealthRepository.findAllByOrderByIntegrationTypeAsc();
        return AdminIntegrationConverter.toHealthSummaryDTO(healths);
    }
}
