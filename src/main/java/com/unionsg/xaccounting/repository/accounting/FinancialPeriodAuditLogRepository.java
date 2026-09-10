package com.unionsg.xaccounting.repository.accounting;

import com.unionsg.xaccounting.entity.accounting.FinancialPeriodAuditLog;
import com.unionsg.xaccounting.enums.FinancialPeriodEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialPeriodAuditLogRepository extends JpaRepository<FinancialPeriodAuditLog, Long> {

    List<FinancialPeriodAuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(
            FinancialPeriodEntityType entityType, Long entityId);
}
