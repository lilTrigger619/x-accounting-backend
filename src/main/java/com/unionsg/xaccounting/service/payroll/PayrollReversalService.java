package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.enums.PayrollAuditAction;
import com.unionsg.xaccounting.enums.PayrollAuditEntityType;
import com.unionsg.xaccounting.enums.PayrollRunStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.payroll.PayrollRunRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Controlled Payroll Run reversal (§30). A posted run is never deleted - reversing it posts an
 * equal-and-opposite counter-entry through the same {@code JournalService.reverse()} every other
 * module in this system uses (§50), which preserves the original journal, requires a reason, and
 * is itself fully audited. The Payroll Run record, its calculated employee records, and its
 * component breakdown are untouched; only its status moves to REVERSED.
 *
 * <p>Known limitation, stated rather than silently skipped: reversing a run does not currently
 * restore the outstanding balances of any employee loan or salary advance whose repayment that
 * run's posting had already reduced (or closed). Undoing those subledger status transitions
 * symmetrically is deferred - reversing payroll after a loan has already been fully repaid is an
 * edge case, and should be corrected manually (a new loan/advance adjustment) until this is
 * built.</p>
 */
@Service
@RequiredArgsConstructor
public class PayrollReversalService {

    private final PayrollRunRepository payrollRunRepository;
    private final JournalService journalService;
    private final PayrollAuditLogService auditLogService;

    @Transactional
    public void reverse(PayrollRun run, String reason) {
        if (run.getStatus() != PayrollRunStatus.POSTED && run.getStatus() != PayrollRunStatus.PAID) {
            throw new BusinessException("Only a POSTED or PAID payroll run can be reversed");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A reason is required to reverse a payroll run");
        }

        if (run.getPaymentJournalId() != null) {
            journalService.reverse(run.getPaymentJournalId(), "Payroll run " + run.getRunNumber() + " reversed: " + reason);
        }
        journalService.reverse(run.getJournalId(), "Payroll run " + run.getRunNumber() + " reversed: " + reason);

        PayrollRunStatus previous = run.getStatus();
        run.setStatus(PayrollRunStatus.REVERSED);
        run.setReversedAt(LocalDateTime.now());
        run.setReversalReason(reason);
        payrollRunRepository.save(run);

        auditLogService.record(PayrollAuditEntityType.PAYROLL_RUN, run.getId(), PayrollAuditAction.REVERSED,
                previous.name(), PayrollRunStatus.REVERSED.name(), reason);
    }
}
