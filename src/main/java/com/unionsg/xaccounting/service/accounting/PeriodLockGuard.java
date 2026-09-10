package com.unionsg.xaccounting.service.accounting;

import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * The single, central enforcement point for Accounting Period controls.
 *
 * <p>Every GL-affecting posting path in the system (manual journals, AR payment/refund
 * postings, AP bill/supplier-payment postings, journal reversal, recurring journal
 * generation) must call {@link #assertPostable(LocalDate)} before creating or modifying
 * a posted accounting entry, so that a locked or permanently closed period can never be
 * bypassed by any module.</p>
 *
 * <p>A date that does not fall within any defined Accounting Period is allowed through —
 * this keeps the guard backward-compatible with dates outside whatever calendar has been
 * configured so far, rather than blocking every transaction the moment this feature ships.</p>
 */
@Service
@RequiredArgsConstructor
public class PeriodLockGuard {

    private final AccountingPeriodRepository accountingPeriodRepository;

    @Transactional(readOnly = true)
    public void assertPostable(LocalDate date) {
        if (date == null) {
            return;
        }
        Optional<AccountingPeriod> period = accountingPeriodRepository.findByDateInRange(date);
        if (period.isEmpty()) {
            return;
        }

        AccountingPeriod p = period.get();
        if (p.getStatus() == AccountingPeriodStatus.LOCKED) {
            throw new BadRequestException(
                    "Accounting period \"" + p.getName() + "\" (" + p.getStartDate() + " to " + p.getEndDate()
                            + ") is locked. Transactions dated " + date + " cannot be posted until it is unlocked."
            );
        }
        if (p.getStatus() == AccountingPeriodStatus.CLOSED) {
            throw new BadRequestException(
                    "Accounting period \"" + p.getName() + "\" (" + p.getStartDate() + " to " + p.getEndDate()
                            + ") is permanently closed. Transactions dated " + date + " cannot be posted."
            );
        }
    }

    /** Read-only check used by callers (e.g. recurring journal generation) that want to
     * decide their own handling instead of an exception. */
    @Transactional(readOnly = true)
    public boolean isPostable(LocalDate date) {
        if (date == null) {
            return true;
        }
        return accountingPeriodRepository.findByDateInRange(date)
                .map(AccountingPeriod::isPostable)
                .orElse(true);
    }
}
