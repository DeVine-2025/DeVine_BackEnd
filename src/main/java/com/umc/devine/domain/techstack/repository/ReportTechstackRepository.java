package com.umc.devine.domain.techstack.repository;

import com.umc.devine.domain.report.entity.DevReport;
import com.umc.devine.domain.techstack.entity.mapping.ReportTechstack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportTechstackRepository extends JpaRepository<ReportTechstack, ReportTechstack.ReportTechstackId> {
    List<ReportTechstack> findAllByDevReport(DevReport devReport);
}
