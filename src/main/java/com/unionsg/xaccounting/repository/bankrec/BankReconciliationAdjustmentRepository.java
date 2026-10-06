package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankReconciliationAdjustment;
import com.unionsg.xaccounting.enums.bankrec.AdjustmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankReconciliationAdjustmentRepository extends JpaRepository<BankReconciliationAdjustment, Long> {

    List<BankReconciliationAdjustment> findByReconciliationIdOrderByIdDesc(Long reconciliationId);

    List<BankReconciliationAdjustment> findByReconciliationIdAndStatus(Long reconciliationId, AdjustmentStatus status);

    boolean existsByReconciliationId(Long reconciliationId);

    boolean existsByStatementTransactionStatementImportId(Long statementImportId);
}
