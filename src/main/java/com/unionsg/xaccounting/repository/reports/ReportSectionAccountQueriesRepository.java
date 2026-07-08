package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportSectionAccount;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ReportSectionAccountQueriesRepository {

    private final ReportSectionAccountRepository repository;

    public ReportSectionAccountQueriesRepository(ReportSectionAccountRepository repository) {
        this.repository = repository;
    }

    public Optional<ReportSectionAccount> findByReportSectionIdAndAccountId(Long reportSectionId, Long accountId) {
        List<ReportSectionAccount> rows = repository.findByReportSectionId(reportSectionId);
        return rows.stream()
                .filter(r -> r.getAccount() != null && r.getAccount().getId() != null)
                .filter(r -> r.getAccount().getId().equals(accountId))
                .findFirst();
    }

    public List<ReportSectionAccount> findByReportSectionId(Long reportSectionId) {
        return repository.findByReportSectionId(reportSectionId);
    }

    public boolean existsByReportSectionIdAndAccountId(Long reportSectionId, Long accountId) {
        return findByReportSectionIdAndAccountId(reportSectionId, accountId).isPresent();
    }
}

