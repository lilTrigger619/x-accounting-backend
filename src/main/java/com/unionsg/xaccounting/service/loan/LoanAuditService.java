package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanAuditLog;
import com.unionsg.xaccounting.enums.loan.LoanAuditAction;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import com.unionsg.xaccounting.repository.loan.LoanAuditLogRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes the append-only loan activity trail. */
@Service
@RequiredArgsConstructor
public class LoanAuditService {

    private final LoanAuditLogRepository repository;

    @Transactional
    public void record(Loan loan, LoanAuditAction action, LoanStatus previousStatus, String details) {
        LoanAuditLog log = new LoanAuditLog();
        log.setLoanId(loan.getId());
        log.setAction(action);
        log.setPreviousStatus(previousStatus != null ? previousStatus.name() : null);
        log.setNewStatus(loan.getStatus() != null ? loan.getStatus().name() : null);
        log.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        log.setPerformedBy(SecurityUtils.getCurrentUser());
        repository.save(log);
    }
}
