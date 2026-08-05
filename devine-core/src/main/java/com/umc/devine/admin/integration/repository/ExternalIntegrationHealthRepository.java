package com.umc.devine.admin.integration.repository;

import com.umc.devine.admin.integration.entity.ExternalIntegrationHealth;
import com.umc.devine.admin.integration.enums.IntegrationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalIntegrationHealthRepository extends JpaRepository<ExternalIntegrationHealth, Long> {

    Optional<ExternalIntegrationHealth> findByIntegrationType(IntegrationType integrationType);

    List<ExternalIntegrationHealth> findAllByOrderByIntegrationTypeAsc();
}
