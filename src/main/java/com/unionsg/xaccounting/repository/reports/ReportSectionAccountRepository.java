package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportSectionAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportSectionAccountRepository extends JpaRepository<ReportSectionAccount, Long> {

    List<ReportSectionAccount> findByReportSectionId(Long reportSectionId);
}

