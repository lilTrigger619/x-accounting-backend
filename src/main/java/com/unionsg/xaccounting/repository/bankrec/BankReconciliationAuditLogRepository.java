package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankReconciliationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankReconciliationAuditLogRepository extends JpaRepository<BankReconciliationAuditLog, Long> {

    List<BankReconciliationAuditLog> findByReconciliationIdOrderByOccurredAtDescIdDesc(Long reconciliationId);

    List<BankReconciliationAuditLog> findByBankAccountIdOrderByOccurredAtDescIdDesc(Long bankAccountId);
}
