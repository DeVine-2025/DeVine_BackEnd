package com.umc.devine.domain.maintenance.repository;

import com.umc.devine.domain.maintenance.entity.MaintenanceSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceSettingRepository extends JpaRepository<MaintenanceSetting, Long> {
}
