package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportSectionRepository extends JpaRepository<ReportSection, Long> {

    List<ReportSection> findByReportDefinitionId(Long reportDefinitionId);
}

