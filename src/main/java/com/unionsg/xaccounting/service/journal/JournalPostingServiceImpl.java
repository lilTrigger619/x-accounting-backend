package com.unionsg.xaccounting.service.journal;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.branch.InterBranchBalancer;
import com.unionsg.xaccounting.service.branch.JournalDimensionResolver;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class JournalPostingServiceImpl implements JournalPostingService {

    private final PeriodLockGuard periodLockGuard;
    private final InterBranchBalancer interBranchBalancer;
    private final JournalDimensionResolver dimensionResolver;

    @Override
    public void validateJournal(JournalEntry journal) {

        validateLines(journal);

        validateBalanced(journal);

        interBranchBalancer.assertBalancedPerBranch(journal);

        validatePostingPeriod(journal);
    }

    @Override
    public void validateBalanced(JournalEntry journal) {

        if (journal.getTotalDebit()
                .compareTo(journal.getTotalCredit()) != 0) {

            throw new BadRequestException(
                    "Journal is not balanced"
            );
        }
    }

    @Override
    public void validateLines(JournalEntry journal) {

        if (journal.getLines() == null
                || journal.getLines().size() < 2) {

            throw new BadRequestException(
                    "Journal must contain at least 2 lines"
            );
        }

        for (JournalLine line : journal.getLines()) {

            BigDecimal debit = line.getDebitAmount();

            BigDecimal credit = line.getCreditAmount();

            boolean hasDebit =
                    debit != null
                            && debit.compareTo(BigDecimal.ZERO) > 0;

            boolean hasCredit =
                    credit != null
                            && credit.compareTo(BigDecimal.ZERO) > 0;

            if (hasDebit == hasCredit) {

                throw new BadRequestException(
                        "Each line must contain either debit or credit"
                );
            }
        }
    }

    @Override
    public void validatePostingPeriod(JournalEntry journal) {

        periodLockGuard.assertPostable(journal.getJournalDate());
    }

    @Override
    public void post(JournalEntry journal) {

        if (journal.getStatus() == JournalStatus.DRAFT) {
            if (Boolean.TRUE.equals(journal.getRequiresApproval())) {
                throw new BadRequestException(
                        "This journal needs approval before it can be posted. Submit it for approval first.");
            }
        } else if (journal.getStatus() != JournalStatus.APPROVED) {
            throw new BadRequestException(
                    "Only draft or approved journals can be posted"
            );
        }

        stampMissingDimensions(journal);

        validateJournal(journal);

        journal.setStatus(JournalStatus.POSTED);

        journal.setPostingDate(LocalDate.now());

        journal.setPostedAt(LocalDateTime.now());
    }

    /**
     * Journals built outside JournalService (opening balances, recurring journals, year-end
     * closing) may reach posting without dimensions; they take the journal's own cost center,
     * else the poster's default, so no posted line is ever without a branch.
     */
    private void stampMissingDimensions(JournalEntry journal) {
        CostCenter fallback = journal.getCostCenter();
        for (JournalLine line : journal.getLines()) {
            if (line.getCostCenter() != null) {
                if (line.getBranch() == null) {
                    line.setBranch(line.getCostCenter().getBranch());
                }
                continue;
            }
            if (fallback == null) {
                fallback = dimensionResolver.currentUserDefault();
            }
            line.setCostCenter(fallback);
            line.setBranch(fallback.getBranch());
        }
        if (journal.getCostCenter() == null && fallback != null) {
            journal.setCostCenter(fallback);
        }
        if (journal.getBranch() == null && journal.getCostCenter() != null) {
            journal.setBranch(journal.getCostCenter().getBranch());
        }
    }
}
