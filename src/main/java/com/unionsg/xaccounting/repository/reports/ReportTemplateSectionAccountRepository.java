package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportTemplateSectionAccountRepository extends JpaRepository<ReportTemplateSectionAccount, Long> {

    List<ReportTemplateSectionAccount> findByReportTemplateSectionId(Long reportTemplateSectionId);

    Optional<ReportTemplateSectionAccount> findByReportTemplateSectionIdAndAccountId(Long reportTemplateSectionId, Long accountId);

    boolean existsByReportTemplateSectionIdAndAccountId(Long reportTemplateSectionId, Long accountId);

    List<ReportTemplateSectionAccount> findByReportTemplateSectionIdOrderByDisplayOrderAsc(Long reportTemplateSectionId);
}

