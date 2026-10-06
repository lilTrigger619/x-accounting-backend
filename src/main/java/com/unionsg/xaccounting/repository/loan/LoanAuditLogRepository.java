package com.unionsg.xaccounting.repository.loan;

import com.unionsg.xaccounting.entity.loan.LoanAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanAuditLogRepository extends JpaRepository<LoanAuditLog, Long> {
    List<LoanAuditLog> findByLoanIdOrderByPerformedAtDescIdDesc(Long loanId);
}
