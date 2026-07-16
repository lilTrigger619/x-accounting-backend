package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportTemplateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportTemplateHistoryRepository extends JpaRepository<ReportTemplateHistory, Long> {

    List<ReportTemplateHistory> findByTemplateIdOrderByPerformedAtAsc(Long templateId);
}

